package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillVersion;
import clawhub.repository.SkillVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityScanService {

    private final SkillVersionRepository versionRepository;
    private final VirusTotalService virusTotalService;
    private final LlmSecurityService llmSecurityService;
    private final StorageService storageService;

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
     * 执行完整的安全扫描
     */
    @Transactional
    public SkillVersion.ModerationSnapshot performSecurityScan(UUID versionId) {
        SkillVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        log.info("Starting security scan for version: {}", versionId);

        // 1. 静态代码扫描
        List<SkillVersion.ModerationSnapshot.ModerationFinding> staticFindings = performStaticScan(version);

        // 2. VirusTotal 扫描
        Optional<SkillVersion.VirusTotalAnalysis> vtAnalysis = performVirusTotalScan(version);

        // 3. LLM 安全评估
        Optional<SkillVersion.LlmSecurityAnalysis> llmAnalysis = performLlmEvaluation(version, staticFindings);

        // 4. 合并结果生成最终裁决
        SkillVersion.ModerationSnapshot snapshot = mergeVerdicts(version, staticFindings, vtAnalysis, llmAnalysis);

        // 更新版本记录
        version.setVtAnalysis(vtAnalysis.orElse(null));
        version.setLlmAnalysis(llmAnalysis.orElse(null));
        version.setModerationSnapshot(snapshot);
        versionRepository.save(version);

        // 更新技能状态
        updateSkillModerationStatus(version.getSkill(), snapshot);

        log.info("Security scan completed for version: {} - Verdict: {}", versionId, snapshot.getVerdict());

        return snapshot;
    }

    /**
     * 静态代码扫描
     */
    private List<SkillVersion.ModerationSnapshot.ModerationFinding> performStaticScan(SkillVersion version) {
        List<SkillVersion.ModerationSnapshot.ModerationFinding> findings = new ArrayList<>();

        // 获取技能文件内容
        List<SkillVersion.FileInfo> files = version.getFiles();
        if (files == null) return findings;

        for (SkillVersion.FileInfo file : files) {
            String content = fetchFileContent(version, file.getPath());
            if (content == null) continue;

            // 检查各种模式
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

    private void checkPattern(List<SkillVersion.ModerationSnapshot.ModerationFinding> findings,
                              String filePath, String content, Pattern pattern,
                              String code, String severity, String message) {
        var matcher = pattern.matcher(content);
        int lineNum = 1;
        int lastMatchEnd = 0;

        while (matcher.find()) {
            // 计算行号
            for (int i = lastMatchEnd; i < matcher.start(); i++) {
                if (content.charAt(i) == '\n') lineNum++;
            }
            lastMatchEnd = matcher.end();

            String evidence = truncate(matcher.group(), 160);

            findings.add(SkillVersion.ModerationSnapshot.ModerationFinding.builder()
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
    private Optional<SkillVersion.VirusTotalAnalysis> performVirusTotalScan(SkillVersion version) {
        try {
            // 下载技能包并计算哈希
            byte[] content = downloadSkillPackage(version);
            String sha256 = calculateSha256(content);

            // 先检查是否已有分析结果
            Optional<SkillVersion.VirusTotalAnalysis> existing = virusTotalService.getAnalysisByHash(sha256);
            if (existing.isPresent()) {
                return existing;
            }

            // 提交新文件
            Optional<String> analysisId = virusTotalService.submitFile(content, version.getSkill().getSlug() + ".zip");
            if (analysisId.isEmpty()) {
                return Optional.empty();
            }

            // 等待并获取结果 (简化版，实际应该异步处理)
            Thread.sleep(30000); // 等待30秒
            return virusTotalService.getAnalysis(analysisId.get());

        } catch (Exception e) {
            log.error("VirusTotal scan failed", e);
            return Optional.empty();
        }
    }

    /**
     * LLM 安全评估
     */
    private Optional<SkillVersion.LlmSecurityAnalysis> performLlmEvaluation(
            SkillVersion version,
            List<SkillVersion.ModerationSnapshot.ModerationFinding> staticFindings) {

        List<String> injectionSignals = staticFindings.stream()
                .filter(f -> "critical".equals(f.getSeverity()) || "warn".equals(f.getSeverity()))
                .map(f -> f.getCode() + ": " + f.getMessage())
                .toList();

        String skillMdContent = fetchFileContent(version, "SKILL.md");

        LlmSecurityService.SkillEvalContext context = new LlmSecurityService.SkillEvalContext(
                version.getSkill().getSlug(),
                version.getSkill().getDisplayName(),
                version.getSkill().getOwner().getId().toString(),
                version.getVersion(),
                version.getCreatedAt(),
                version.getSkill().getSummary(),
                null,
                null,
                version.getParsedMetadata(),
                version.getFiles(),
                skillMdContent,
                injectionSignals
        );

        return llmSecurityService.evaluateSkill(context);
    }

    /**
     * 合并扫描结果生成最终裁决
     */
    private SkillVersion.ModerationSnapshot mergeVerdicts(
            SkillVersion version,
            List<SkillVersion.ModerationSnapshot.ModerationFinding> staticFindings,
            Optional<SkillVersion.VirusTotalAnalysis> vtAnalysis,
            Optional<SkillVersion.LlmSecurityAnalysis> llmAnalysis) {

        List<String> reasonCodes = new ArrayList<>();
        String verdict = "clean";

        // 分析静态扫描结果
        long criticalCount = staticFindings.stream().filter(f -> "critical".equals(f.getSeverity())).count();
        long warnCount = staticFindings.stream().filter(f -> "warn".equals(f.getSeverity())).count();

        if (criticalCount > 0) {
            verdict = "suspicious";
            reasonCodes.add("STATIC_CRITICAL_FINDINGS");
        } else if (warnCount > 2) {
            verdict = "suspicious";
            reasonCodes.add("STATIC_MULTIPLE_WARNINGS");
        }

        // 分析 VirusTotal 结果
        if (vtAnalysis.isPresent()) {
            SkillVersion.VirusTotalAnalysis vt = vtAnalysis.get();
            if (vt.getMaliciousCount() != null && vt.getMaliciousCount() > 0) {
                verdict = "malicious";
                reasonCodes.add("VT_MALICIOUS_DETECTED");
            } else if (vt.getSuspiciousCount() != null && vt.getSuspiciousCount() > 0) {
                verdict = "suspicious";
                reasonCodes.add("VT_SUSPICIOUS_DETECTED");
            }
        }

        // 分析 LLM 结果
        if (llmAnalysis.isPresent()) {
            SkillVersion.LlmSecurityAnalysis llm = llmAnalysis.get();
            if ("malicious".equals(llm.getVerdict()) && "high".equals(llm.getConfidence())) {
                verdict = "malicious";
                reasonCodes.add("LLM_MALICIOUS_HIGH_CONFIDENCE");
            } else if ("suspicious".equals(llm.getVerdict()) && "high".equals(llm.getConfidence())) {
                if (!"malicious".equals(verdict)) {
                    verdict = "suspicious";
                }
                reasonCodes.add("LLM_SUSPICIOUS_HIGH_CONFIDENCE");
            }
        }

        // 生成摘要
        String summary = generateSummary(verdict, reasonCodes, staticFindings.size(),
                vtAnalysis.map(SkillVersion.VirusTotalAnalysis::getMaliciousCount).orElse(0));

        return SkillVersion.ModerationSnapshot.builder()
                .verdict(verdict)
                .reasonCodes(reasonCodes)
                .evidence(staticFindings)
                .summary(summary)
                .engineVersion("1.0.0")
                .evaluatedAt(Instant.now())
                .sourceVersionId(version.getId())
                .build();
    }

    private String generateSummary(String verdict, List<String> reasonCodes,
                                   int staticFindingCount, int vtMaliciousCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Verdict: ").append(verdict.toUpperCase()).append(". ");
        sb.append("Static scan found ").append(staticFindingCount).append(" issues. ");

        if (vtMaliciousCount > 0) {
            sb.append("VirusTotal detected ").append(vtMaliciousCount).append(" malicious engines. ");
        }

        if (!reasonCodes.isEmpty()) {
            sb.append("Reasons: ").append(String.join(", ", reasonCodes));
        }

        return sb.toString();
    }

    /**
     * 更新技能审核状态
     */
    private void updateSkillModerationStatus(Skill skill, SkillVersion.ModerationSnapshot snapshot) {
        skill.setModerationVerdict(snapshot.getVerdict());
        skill.setModerationFlags(snapshot.getReasonCodes());

        switch (snapshot.getVerdict()) {
            case "malicious":
                skill.setModerationStatus(Skill.ModerationStatus.HIDDEN);
                break;
            case "suspicious":
                // 可疑技能保持可见但标记
                break;
            case "clean":
                skill.setModerationStatus(Skill.ModerationStatus.ACTIVE);
                break;
        }
    }

    // Helper methods

    private String fetchFileContent(SkillVersion version, String path) {
        try {
            return storageService.readFile(version.getSkill().getId(), version.getId(), path);
        } catch (Exception e) {
            log.debug("Failed to read file: {}", path);
            return null;
        }
    }

    private byte[] downloadSkillPackage(SkillVersion version) {
        try {
            return storageService.downloadSkillPackage(version.getSkill().getId(), version.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download skill package", e);
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