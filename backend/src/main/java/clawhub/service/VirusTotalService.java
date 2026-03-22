package clawhub.service;

import clawhub.entity.SkillVersion;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirusTotalService {

    private final WebClient.Builder webClientBuilder;

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
}
