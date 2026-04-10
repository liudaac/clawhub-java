package clawhub.service;

import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import clawhub.repository.PackageReleaseRepository;
import clawhub.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageSecurityScanService {

    private final PackageRepository packageRepository;
    private final PackageReleaseRepository releaseRepository;
    private final VirusTotalService virusTotalService;
    private final LlmSecurityService llmSecurityService;
    private final StorageService storageService;
    private final PackageSecurityService packageSecurityService;

    // Static scan patterns
    private static final Pattern RAW_IP_URL_PATTERN = Pattern.compile(
            "https?://\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?(?:/|[\"'])",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CURL_PIPE_PATTERN = Pattern.compile(
            "(?:curl|wget)\\b[^\\n|]{0,240}\\|\\s*(?:/bin/)?(?:ba)?sh\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BASE64_EXEC_PATTERN = Pattern.compile(
            "(?:echo|printf)\\s+[\"'][A-Za-z0-9+/=\\s]{40,}[\"']\\s*\\|\\s*base64\\s+-?[dD]\\b[^\\n|]{0,120}\\|\\s*(?:/bin/)?(?:ba)?sh\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CHILD_PROCESS_PATTERN = Pattern.compile(
            "child_process",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXEC_PATTERN = Pattern.compile(
            "\\b(exec|execSync|spawn|spawnSync|execFile|execFileSync)\\s*\\(",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ENV_VAR_PATTERN = Pattern.compile(
            "process\\.env\\.",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "(api[_-]?key|apikey|api[_-]?secret|apisecret|auth[_-]?token|private[_-]?key)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 异步执行完整的安全扫描
     */
    @Async("securityScanExecutor")
    @Transactional
    public void performSecurityScanAsync(UUID releaseId) {
        try {
            performSecurityScan(releaseId);
        } catch (Exception e) {
            log.error("Async security scan failed for release: {}", releaseId, e);
        }
    }

    /**
     * 执行完整的安全扫描
     */
    @Transactional
    public PackageRelease performSecurityScan(UUID releaseId) {
        PackageRelease release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release not found: " + releaseId));

        log.info("Starting security scan for release: {} (package: {} v{})",
                releaseId, release.getPackage_().getName(), release.getVersion());

        // Update scan status
        Package pkg = release.getPackage_();
        pkg.setScanStatus(Package.ScanStatus.PENDING);
        packageRepository.save(pkg);

        try {
            // 1. 静态代码扫描
            List<PackageRelease.StaticScanResult.Finding> staticFindings = performStaticScan(release);
            PackageRelease.StaticScanResult staticScan = PackageRelease.StaticScanResult.builder()
                    .status("completed")
                    .findings(staticFindings)
                    .scannedAt(Instant.now())
                    .scannerVersion("1.0.0")
                    .build();
            release.setStaticScan(staticScan);

            // 2. VirusTotal 扫描 - 使用新的调度方法（带初始延迟）
            virusTotalService.schedulePackageScan(release.getId(), pkg.getId());
            // 注意：VT扫描现在是异步的，不会立即返回结果
            // 这里我们设置一个临时的 PENDING 状态
            release.setVtAnalysis(null); // 将在异步扫描完成后更新

            // 3. LLM 安全评估
            Optional<PackageRelease.LlmSecurityAnalysis> llmAnalysis = performLlmEvaluation(release, staticFindings);
            release.setLlmAnalysis(llmAnalysis.orElse(null));

            // 4. 使用新的集中化状态解析服务
            // 更新发布版本的数据
            release.setStaticScan(staticScan);
            release.setLlmAnalysis(llmAnalysis.orElse(null));

            // 使用 PackageSecurityService 解析最终状态
            PackageSecurityService.ScanStatus finalStatus =
                packageSecurityService.resolvePackageReleaseScanStatus(release);

            // Update package scan status
            pkg.setScanStatus(mapServiceStatusToEntityStatus(finalStatus));
            packageRepository.save(pkg);

            // 同步验证状态
            packageSecurityService.syncLatestPackageVerification(release);

            // Save release
            PackageRelease saved = releaseRepository.save(release);

            log.info("Security scan completed for release: {} - Status: {}", releaseId, finalStatus);

            return saved;
        } catch (Exception e) {
            log.error("Security scan failed for release: {}", releaseId, e);
            pkg.setScanStatus(Package.ScanStatus.NOT_RUN);
            packageRepository.save(pkg);
            throw e;
        }
    }

    /**
     * 静态代码扫描
     */
    private List<PackageRelease.StaticScanResult.Finding> performStaticScan(PackageRelease release) {
        List<PackageRelease.StaticScanResult.Finding> findings = new ArrayList<>();

        List<PackageRelease.FileInfo> files = release.getFiles();
        if (files == null) return findings;

        for (PackageRelease.FileInfo file : files) {
            String content = fetchFileContent(release, file.getPath());
            if (content == null) continue;

            checkPattern(findings, file.getPath(), content, RAW_IP_URL_PATTERN,
                    "RAW_IP_URL", "warn", "URL contains raw IP address");

            checkPattern(findings, file.getPath(), content, CURL_PIPE_PATTERN,
                    "CURL_PIPE_SHELL", "critical", "Downloads and executes shell script");

            checkPattern(findings, file.getPath(), content, BASE64_EXEC_PATTERN,
                    "BASE64_EXEC", "critical", "Executes base64-encoded commands");

            if (isCodeFile(file.getPath())) {
                checkPattern(findings, file.getPath(), content, CHILD_PROCESS_PATTERN,
                        "CHILD_PROCESS", "warn", "Uses child_process module");

                checkPattern(findings, file.getPath(), content, EXEC_PATTERN,
                        "DYNAMIC_EXECUTION", "critical", "Executes dynamic code");
            }

            checkPattern(findings, file.getPath(), content, ENV_VAR_PATTERN,
                    "ENV_ACCESS", "info", "Accesses environment variables");

            checkPattern(findings, file.getPath(), content, API_KEY_PATTERN,
                    "API_KEY_MENTION", "warn", "Mentions API keys in code");
        }

        return findings;
    }

    private void checkPattern(List<PackageRelease.StaticScanResult.Finding> findings,
                              String filePath, String content, Pattern pattern,
                              String code, String severity, String message) {
        var matcher = pattern.matcher(content);
        int lineNum = 1;
        int lastMatchEnd = 0;

        while (matcher.find()) {
            for (int i = lastMatchEnd; i < matcher.start(); i++) {
                if (content.charAt(i) == '\n') lineNum++;
            }
            lastMatchEnd = matcher.end();

            String evidence = truncate(matcher.group(), 160);

            findings.add(PackageRelease.StaticScanResult.Finding.builder()
                    .code(code)
                    .severity(severity)
                    .file(filePath)
                    .line(lineNum)
                    .message(message)
                    .evidence(evidence)
                    .build());
        }
    }

    /**
     * VirusTotal 扫描
     */
    private Optional<PackageRelease.VirusTotalAnalysis> performVirusTotalScan(PackageRelease release) {
        try {
            // 下载包内容并计算哈希
            byte[] content = downloadPackageContent(release);
            String sha256 = calculateSha256(content);

            // 先检查是否已有分析结果
            Optional<PackageRelease.VirusTotalAnalysis> existing = virusTotalService.getAnalysisByHash(sha256);
            if (existing.isPresent()) {
                return existing;
            }

            // 提交新文件
            Optional<String> analysisId = virusTotalService.submitFile(content, 
                    release.getPackage_().getName() + "-" + release.getVersion() + ".zip");
            if (analysisId.isEmpty()) {
                return Optional.empty();
            }

            // 等待并获取结果 (简化版，实际应该异步处理)
            Thread.sleep(30000);
            return virusTotalService.getAnalysis(analysisId.get());

        } catch (Exception e) {
            log.error("VirusTotal scan failed", e);
            return Optional.empty();
        }
    }

    /**
     * LLM 安全评估
     */
    private Optional<PackageRelease.LlmSecurityAnalysis> performLlmEvaluation(
            PackageRelease release,
            List<PackageRelease.StaticScanResult.Finding> staticFindings) {

        List<String> injectionSignals = staticFindings.stream()
                .filter(f -> "critical".equals(f.getSeverity()) || "warn".equals(f.getSeverity()))
                .map(f -> f.getCode() + ": " + f.getMessage())
                .toList();

        // Build context for LLM evaluation
        StringBuilder context = new StringBuilder();
        context.append("Package: ").append(release.getPackage_().getName()).append("\n");
        context.append("Display Name: ").append(release.getPackage_().getDisplayName()).append("\n");
        context.append("Version: ").append(release.getVersion()).append("\n");
        context.append("Family: ").append(release.getPackage_().getFamily()).append("\n");
        context.append("Summary: ").append(release.getPackage_().getSummary()).append("\n\n");

        context.append("Files:\n");
        if (release.getFiles() != null) {
            for (PackageRelease.FileInfo file : release.getFiles()) {
                context.append("  - ").append(file.getPath()).append(" (").append(file.getSize()).append(" bytes)\n");
            }
        }
        context.append("\n");

        if (!injectionSignals.isEmpty()) {
            context.append("Static scan findings:\n");
            for (String signal : injectionSignals) {
                context.append("  - ").append(signal).append("\n");
            }
        }

        // Call LLM service
        return llmSecurityService.evaluatePackage(context.toString(), release.getPackage_().getFamily().name());
    }

    /**
     * 合并扫描结果生成最终裁决
     */
    private String mergeVerdicts(
            List<PackageRelease.StaticScanResult.Finding> staticFindings,
            Optional<PackageRelease.VirusTotalAnalysis> vtAnalysis,
            Optional<PackageRelease.LlmSecurityAnalysis> llmAnalysis) {

        long criticalCount = staticFindings.stream().filter(f -> "critical".equals(f.getSeverity())).count();

        // Check VirusTotal
        if (vtAnalysis.isPresent()) {
            PackageRelease.VirusTotalAnalysis vt = vtAnalysis.get();
            if (vt.getMaliciousCount() != null && vt.getMaliciousCount() > 0) {
                return "malicious";
            }
            if (vt.getSuspiciousCount() != null && vt.getSuspiciousCount() > 0) {
                return "suspicious";
            }
        }

        // Check LLM
        if (llmAnalysis.isPresent()) {
            PackageRelease.LlmSecurityAnalysis llm = llmAnalysis.get();
            if ("malicious".equals(llm.getVerdict()) && "high".equals(llm.getConfidence())) {
                return "malicious";
            }
            if ("suspicious".equals(llm.getVerdict()) && "high".equals(llm.getConfidence())) {
                return "suspicious";
            }
        }

        // Check static findings
        if (criticalCount > 0) {
            return "suspicious";
        }

        return "clean";
    }

    private Package.ScanStatus mapVerdictToScanStatus(String verdict) {
        return switch (verdict) {
            case "malicious" -> Package.ScanStatus.MALICIOUS;
            case "suspicious" -> Package.ScanStatus.SUSPICIOUS;
            case "clean" -> Package.ScanStatus.CLEAN;
            default -> Package.ScanStatus.NOT_RUN;
        };
    }

    /**
     * 映射服务层 ScanStatus 到实体层 ScanStatus
     */
    private Package.ScanStatus mapServiceStatusToEntityStatus(PackageSecurityService.ScanStatus status) {
        return switch (status) {
            case CLEAN -> Package.ScanStatus.CLEAN;
            case SUSPICIOUS -> Package.ScanStatus.SUSPICIOUS;
            case MALICIOUS -> Package.ScanStatus.MALICIOUS;
            case PENDING -> Package.ScanStatus.PENDING;
            case NOT_RUN -> Package.ScanStatus.NOT_RUN;
        };
    }

    // Helper methods

    private String fetchFileContent(PackageRelease release, String path) {
        try {
            return storageService.readFile(release.getPackage_().getId(), release.getId(), path);
        } catch (Exception e) {
            log.debug("Failed to read file: {}", path);
            return null;
        }
    }

    private byte[] downloadPackageContent(PackageRelease release) {
        try {
            return storageService.downloadPackage(release.getPackage_().getId(), release.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download package", e);
        }
    }

    private String calculateSha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private boolean isCodeFile(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".py") ||
               lower.endsWith(".sh") || lower.endsWith(".bash") || lower.endsWith(".go") ||
               lower.endsWith(".rb") || lower.endsWith(".java");
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}