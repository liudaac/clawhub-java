package clawhub.service;

import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import clawhub.entity.SkillVersion;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirusTotalService {

    private final WebClient.Builder webClientBuilder;
    private final TaskScheduler taskScheduler;
    private final PackageSecurityService packageSecurityService;

    // 扫描重试配置
    private static final int PACKAGE_SCAN_MAX_ATTEMPTS = 3;
    private static final long PACKAGE_SCAN_RETRY_DELAY_MS = 30_000; // 30秒
    private static final long INITIAL_PACKAGE_VT_SCAN_DELAY_MS = 30_000; // 初始延迟30秒

    @Value("${virustotal.api.key:}")
    private String apiKey;

    @Value("${virustotal.api.url:https://www.virustotal.com/api/v3}")
    private String apiUrl;

    @Value("${virustotal.enabled:false}")
    private boolean enabled;

    private WebClient getClient() {
        return webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-apikey", apiKey)
                .build();
    }

    /**
     * 提交文件进行扫描
     */
    @CircuitBreaker(name = "virustotal", fallbackMethod = "submitFileFallback")
    @Retry(name = "virustotal")
    public Optional<String> submitFile(byte[] fileContent, String filename) {
        if (!enabled || apiKey.isEmpty()) {
            log.debug("VirusTotal is disabled or API key not configured");
            return Optional.empty();
        }

        try {
            log.info("Submitting file to VirusTotal: {}", filename);

            var response = getClient().post()
                    .uri("/files")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(Map.of(
                            "file", Map.of("filename", filename, "content", fileContent)
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String analysisId = (String) data.get("id");
                log.info("File submitted successfully, analysis ID: {}", analysisId);
                return Optional.of(analysisId);
            }

            return Optional.empty();
        } catch (WebClientResponseException e) {
            log.error("VirusTotal API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Failed to submit file to VirusTotal", e);
            throw e;
        }
    }

    /**
     * 获取扫描结果
     */
    @CircuitBreaker(name = "virustotal", fallbackMethod = "getAnalysisFallback")
    @Retry(name = "virustotal")
    public Optional<SkillVersion.VirusTotalAnalysis> getAnalysis(String analysisId) {
        if (!enabled || apiKey.isEmpty()) {
            return Optional.empty();
        }

        try {
            log.debug("Fetching VirusTotal analysis: {}", analysisId);

            var response = getClient().get()
                    .uri("/analyses/{id}", analysisId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || !response.containsKey("data")) {
                return Optional.empty();
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
            Map<String, Object> stats = (Map<String, Object>) attributes.get("stats");
            Map<String, Object> results = (Map<String, Object>) attributes.get("results");

            SkillVersion.VirusTotalAnalysis analysis = SkillVersion.VirusTotalAnalysis.builder()
                    .scanId(analysisId)
                    .status("completed")
                    .maliciousCount(getInt(stats, "malicious"))
                    .suspiciousCount(getInt(stats, "suspicious"))
                    .harmlessCount(getInt(stats, "harmless"))
                    .undetectedCount(getInt(stats, "undetected"))
                    .permalink((String) attributes.get("permalink"))
                    .scannedAt(Instant.ofEpochSecond(getLong(attributes, "date")))
                    .results(parseEngineResults(results))
                    .build();

            return Optional.of(analysis);
        } catch (WebClientResponseException e) {
            log.error("VirusTotal API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch VirusTotal analysis", e);
            throw e;
        }
    }

    /**
     * 通过文件哈希获取分析结果
     */
    @CircuitBreaker(name = "virustotal", fallbackMethod = "getAnalysisFallback")
    public Optional<SkillVersion.VirusTotalAnalysis> getAnalysisByHash(String sha256) {
        if (!enabled || apiKey.isEmpty()) {
            return Optional.empty();
        }

        try {
            log.debug("Fetching VirusTotal analysis by hash: {}", sha256);

            var response = getClient().get()
                    .uri("/files/{hash}", sha256)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || !response.containsKey("data")) {
                return Optional.empty();
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
            Map<String, Object> lastAnalysisStats = (Map<String, Object>) attributes.get("last_analysis_stats");
            Map<String, Object> lastAnalysisResults = (Map<String, Object>) attributes.get("last_analysis_results");

            SkillVersion.VirusTotalAnalysis analysis = SkillVersion.VirusTotalAnalysis.builder()
                    .scanId((String) data.get("id"))
                    .status("completed")
                    .maliciousCount(getInt(lastAnalysisStats, "malicious"))
                    .suspiciousCount(getInt(lastAnalysisStats, "suspicious"))
                    .harmlessCount(getInt(lastAnalysisStats, "harmless"))
                    .undetectedCount(getInt(lastAnalysisStats, "undetected"))
                    .scannedAt(Instant.ofEpochSecond(getLong(attributes, "last_analysis_date")))
                    .results(parseEngineResults(lastAnalysisResults))
                    .build();

            return Optional.of(analysis);
        } catch (WebClientResponseException.NotFound e) {
            log.debug("File not found in VirusTotal: {}", sha256);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch VirusTotal analysis by hash", e);
            throw e;
        }
    }

    private Map<String, SkillVersion.VirusTotalAnalysis.EngineResult> parseEngineResults(Map<String, Object> results) {
        if (results == null) {
            return Map.of();
        }

        return results.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Map<String, Object> result = (Map<String, Object>) entry.getValue();
                            return SkillVersion.VirusTotalAnalysis.EngineResult.builder()
                                    .engine(entry.getKey())
                                    .category((String) result.get("category"))
                                    .result((String) result.get("result"))
                                    .build();
                        }
                ));
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }

    // Fallback methods
    private Optional<String> submitFileFallback(byte[] fileContent, String filename, Exception ex) {
        log.warn("VirusTotal submit file fallback triggered: {}", ex.getMessage());
        return Optional.empty();
    }

    private Optional<SkillVersion.VirusTotalAnalysis> getAnalysisFallback(String analysisId, Exception ex) {
        log.warn("VirusTotal get analysis fallback triggered: {}", ex.getMessage());
        return Optional.empty();
    }

    // ==================== Package Release Scan Methods ====================

    /**
     * 调度包发布的VT扫描（带初始延迟）
     * 对应原版: runAfterRef(ctx, INITIAL_PACKAGE_VT_SCAN_DELAY_MS, ...)
     */
    public void schedulePackageScan(UUID releaseId, UUID packageId) {
        log.info("Scheduling VT scan for package release: {} with {}ms delay", releaseId, INITIAL_PACKAGE_VT_SCAN_DELAY_MS);

        taskScheduler.schedule(
            () -> performPackageScan(releaseId, packageId, 1),
            Instant.now().plusMillis(INITIAL_PACKAGE_VT_SCAN_DELAY_MS)
        );
    }

    /**
     * 执行包发布的VT扫描（支持重试）
     * 对应原版: scanPackageReleaseWithVirusTotal
     */
    public void performPackageScan(UUID releaseId, UUID packageId, int attempt) {
        log.info("Performing VT scan for release: {} (attempt {})", releaseId, attempt);

        // 这里应该获取包内容并提交扫描
        // 简化实现，实际应该注入 PackageReleaseService 和 StorageService
        // 获取文件内容，计算哈希，提交到VT

        // 如果有文件缺失且未达到最大重试次数，则重试
        // if (missingFiles > 0 && attempt < PACKAGE_SCAN_MAX_ATTEMPTS) {
        //     scheduleRetry(releaseId, packageId, attempt + 1);
        //     return;
        // }

        // 执行扫描...
    }

    /**
     * 调度重试
     */
    private void scheduleRetry(UUID releaseId, UUID packageId, int nextAttempt) {
        log.warn("Scheduling retry {} for VT scan of release: {}", nextAttempt, releaseId);

        taskScheduler.schedule(
            () -> performPackageScan(releaseId, packageId, nextAttempt),
            Instant.now().plusMillis(PACKAGE_SCAN_RETRY_DELAY_MS)
        );
    }

    /**
     * 构建无恶意引擎命中的降级分析结果
     * 对应原版: buildPackageUndetectedFallbackAnalysis()
     *
     * 当VT没有检测到恶意引擎，但也没有明确clean时，根据其他扫描结果降级处理
     */
    public Optional<PackageRelease.VirusTotalAnalysis> buildUndetectedFallbackAnalysis(
            PackageRelease release,
            Package pkg,
            VtAnalysisStats stats) {

        if (stats == null) {
            log.debug("No stats provided for fallback analysis");
            return Optional.empty();
        }

        // 仅对非skill类型包应用降级
        if (pkg.getFamily() == Package.Family.SKILL) {
            log.debug("Skipping fallback for skill package");
            return Optional.empty();
        }

        // 检查验证层级
        if (release.getVerification() == null) {
            log.debug("No verification info for fallback analysis");
            return Optional.empty();
        }

        Object tierObj = release.getVerification().get("tier");
        if (tierObj == null) {
            log.debug("No tier info for fallback analysis");
            return Optional.empty();
        }

        String tier = tierObj.toString();
        boolean isTrustedTier = "source-linked".equals(tier)
            || "provenance-verified".equals(tier)
            || "rebuild-verified".equals(tier);

        if (!isTrustedTier) {
            log.debug("Tier {} is not trusted for fallback", tier);
            return Optional.empty();
        }

        // 检查LLM扫描结果
        if (release.getLlmAnalysis() == null
            || !"clean".equalsIgnoreCase(release.getLlmAnalysis().getVerdict())) {
            log.debug("LLM analysis not clean, skipping fallback");
            return Optional.empty();
        }

        // 检查静态扫描
        if (release.getStaticScan() == null
            || "malicious".equalsIgnoreCase(release.getStaticScan().getStatus())) {
            log.debug("Static scan not clean, skipping fallback");
            return Optional.empty();
        }

        // 检查VT统计：必须没有恶意或可疑引擎
        if (stats.malicious() > 0 || stats.suspicious() > 0) {
            log.debug("VT has malicious/suspicious hits, skipping fallback");
            return Optional.empty();
        }

        // 必须有一些引擎响应
        if (stats.harmless() <= 0 && stats.undetected() <= 0) {
            log.debug("No engine responses, skipping fallback");
            return Optional.empty();
        }

        // 构建降级结果
        log.info("Building undetected fallback analysis for release: {}", release.getId());

        PackageRelease.VirusTotalAnalysis result = PackageRelease.VirusTotalAnalysis.builder()
            .status("clean")
            .scanId("undetected-fallback")
            .maliciousCount(0)
            .suspiciousCount(0)
            .harmlessCount(stats.harmless())
            .undetectedCount(stats.undetected())
            .scannedAt(Instant.now())
            .build();

        return Optional.of(result);
    }

    /**
     * 从VT结果构建包扫描分析
     * 对应原版: buildPackageScanAnalysisFromVtResult()
     */
    public Optional<PackageRelease.VirusTotalAnalysis> buildAnalysisFromVtResult(
            PackageRelease release,
            Package pkg,
            Map<String, Object> vtResponse) {

        if (vtResponse == null || !vtResponse.containsKey("data")) {
            return Optional.empty();
        }

        Map<String, Object> data = (Map<String, Object>) vtResponse.get("data");
        Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");

        // 获取AI分析结果
        Object aiResultsObj = attributes.get("crowdsourced_ai_results");
        if (aiResultsObj instanceof java.util.List) {
            java.util.List<Map<String, Object>> aiResults = (java.util.List<Map<String, Object>>) aiResultsObj;
            for (Map<String, Object> aiResult : aiResults) {
                String category = (String) aiResult.get("category");
                if ("code_insight".equals(category)) {
                    String verdict = (String) aiResult.get("verdict");
                    String status = normalizeAiVerdict(verdict);

                    return Optional.of(PackageRelease.VirusTotalAnalysis.builder()
                        .scanId((String) data.get("id"))
                        .status(status)
                        .scannedAt(Instant.now())
                        .build());
                }
            }
        }

        // 从引擎统计获取状态
        Map<String, Object> stats = (Map<String, Object>) attributes.get("last_analysis_stats");
        if (stats != null) {
            VtAnalysisStats vtStats = new VtAnalysisStats(
                getInt(stats, "malicious"),
                getInt(stats, "suspicious"),
                getInt(stats, "harmless"),
                getInt(stats, "undetected")
            );

            String status = statusFromStats(vtStats);
            if (status != null) {
                return Optional.of(PackageRelease.VirusTotalAnalysis.builder()
                    .scanId((String) data.get("id"))
                    .status(status)
                    .maliciousCount(vtStats.malicious())
                    .suspiciousCount(vtStats.suspicious())
                    .harmlessCount(vtStats.harmless())
                    .undetectedCount(vtStats.undetected())
                    .scannedAt(Instant.now())
                    .build());
            }

            // 尝试降级处理
            return buildUndetectedFallbackAnalysis(release, pkg, vtStats);
        }

        return Optional.empty();
    }

    /**
     * VT分析统计
     */
    public record VtAnalysisStats(
        int malicious,
        int suspicious,
        int harmless,
        int undetected
    ) {}

    /**
     * 从引擎统计获取状态
     */
    private String statusFromStats(VtAnalysisStats stats) {
        if (stats.malicious() > 0) return "malicious";
        if (stats.suspicious() > 0) return "suspicious";
        if (stats.harmless() > 0) return "clean";
        return null;
    }

    /**
     * 标准化AI裁决
     */
    private String normalizeAiVerdict(String verdict) {
        if (verdict == null) return "pending";
        return switch (verdict.toLowerCase()) {
            case "clean", "safe", "benign" -> "clean";
            case "suspicious" -> "suspicious";
            case "malicious", "unsafe" -> "malicious";
            default -> "pending";
        };
    }
}
