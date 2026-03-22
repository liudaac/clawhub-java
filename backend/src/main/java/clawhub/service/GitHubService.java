package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillVersion;
import clawhub.entity.Soul;
import clawhub.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    @Value("${github.api.token:}")
    private String githubToken;

    @Value("${github.backup.enabled:false}")
    private boolean backupEnabled;

    @Value("${github.backup.repo:}")
    private String backupRepo;

    private static final String GITHUB_API_URL = "https://api.github.com";

    private WebClient getClient() {
        WebClient.Builder builder = webClientBuilder.baseUrl(GITHUB_API_URL);
        if (!githubToken.isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "token " + githubToken);
        }
        return builder
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 备份技能到 GitHub
     */
    @CircuitBreaker(name = "github", fallbackMethod = "backupFallback")
    @Retry(name = "github")
    public boolean backupSkill(Skill skill, SkillVersion version, User user) {
        if (!backupEnabled || backupRepo.isEmpty()) {
            log.debug("GitHub backup is disabled");
            return false;
        }

        try {
            String path = buildBackupPath(skill, version);
            byte[] content = createSkillBackup(skill, version);
            String base64Content = Base64.getEncoder().encodeToString(content);

            // 获取当前文件 SHA（如果存在）
            Optional<String> existingSha = getFileSha(path);

            // 构建请求体
            Map<String, Object> request = new HashMap<>();
            request.put("message", buildCommitMessage(skill, version, user));
            request.put("content", base64Content);
            existingSha.ifPresent(sha -> request.put("sha", sha));

            // 上传文件
            var response = getClient().put()
                    .uri("/repos/{owner}/{repo}/contents/{path}",
                            getRepoOwner(), getRepoName(), path)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if (response != null) {
                log.info("Skill backed up to GitHub: {} at {}", skill.getSlug(), path);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to backup skill to GitHub: {}", skill.getSlug(), e);
            throw e;
        }
    }

    /**
     * 备份 Soul 到 GitHub
     */
    @CircuitBreaker(name = "github", fallbackMethod = "backupFallback")
    @Retry(name = "github")
    public boolean backupSoul(Soul soul, User user) {
        if (!backupEnabled || backupRepo.isEmpty()) {
            return false;
        }

        try {
            String path = buildSoulBackupPath(soul);
            byte[] content = createSoulBackup(soul);
            String base64Content = Base64.getEncoder().encodeToString(content);

            Optional<String> existingSha = getFileSha(path);

            Map<String, Object> request = new HashMap<>();
            request.put("message", "Backup soul: " + soul.getSlug());
            request.put("content", base64Content);
            existingSha.ifPresent(sha -> request.put("sha", sha));

            var response = getClient().put()
                    .uri("/repos/{owner}/{repo}/contents/{path}",
                            getRepoOwner(), getRepoName(), path)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if (response != null) {
                log.info("Soul backed up to GitHub: {} at {}", soul.getSlug(), path);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to backup soul to GitHub: {}", soul.getSlug(), e);
            throw e;
        }
    }

    /**
     * 从 GitHub 导入技能
     */
    @CircuitBreaker(name = "github", fallbackMethod = "importFallback")
    public Optional<byte[]> importFromGitHub(String owner, String repo, String path, String ref) {
        try {
            String url = String.format("/repos/%s/%s/contents/%s", owner, repo, path);
            if (ref != null && !ref.isEmpty()) {
                url += "?ref=" + ref;
            }

            var response = getClient().get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || !response.containsKey("content")) {
                return Optional.empty();
            }

            String content = (String) response.get("content");
            String encoding = (String) response.get("encoding");

            if ("base64".equals(encoding)) {
                content = content.replaceAll("\\s", "");
                return Optional.of(Base64.getDecoder().decode(content));
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to import from GitHub: {}/{}/{}", owner, repo, path, e);
            throw e;
        }
    }

    /**
     * 获取 GitHub 仓库信息
     */
    @CircuitBreaker(name = "github")
    public Optional<JsonNode> getRepositoryInfo(String owner, String repo) {
        try {
            var response = getClient().get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null) {
                return Optional.of(objectMapper.readTree(response));
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get repository info: {}/{}", owner, repo, e);
            return Optional.empty();
        }
    }

    /**
     * 获取用户 GitHub 账号信息
     */
    @CircuitBreaker(name = "github")
    public Optional<JsonNode> getUserInfo(String username) {
        try {
            var response = getClient().get()
                    .uri("/users/{username}", username)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null) {
                return Optional.of(objectMapper.readTree(response));
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get user info: {}", username, e);
            return Optional.empty();
        }
    }

    /**
     * 批量恢复技能
     */
    public int bulkRestoreFromBackup(List<String> skillSlugs, User admin) {
        int restored = 0;
        for (String slug : skillSlugs) {
            try {
                Optional<byte[]> content = restoreSkillFromBackup(slug);
                if (content.isPresent()) {
                    restored++;
                    log.info("Restored skill from backup: {}", slug);
                }
            } catch (Exception e) {
                log.error("Failed to restore skill: {}", slug, e);
            }
        }
        return restored;
    }

    /**
     * 从备份恢复技能
     */
    private Optional<byte[]> restoreSkillFromBackup(String slug) {
        String path = "skills/" + slug + "/latest.zip";
        return importFromGitHub(getRepoOwner(), getRepoName(), path, null);
    }

    // Helper methods

    private String buildBackupPath(Skill skill, SkillVersion version) {
        return String.format("skills/%s/%s/%s.zip",
                skill.getSlug(),
                version.getVersion(),
                DateTimeFormatter.ISO_INSTANT.format(version.getCreatedAt()));
    }

    private String buildSoulBackupPath(Soul soul) {
        return String.format("souls/%s/%s.json",
                soul.getSlug(),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
    }

    private String buildCommitMessage(Skill skill, SkillVersion version, User user) {
        return String.format("Backup %s v%s by @%s",
                skill.getSlug(), version.getVersion(), user.getHandle());
    }

    private byte[] createSkillBackup(Skill skill, SkillVersion version) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 添加元数据文件
            String metadata = buildMetadataJson(skill, version);
            ZipEntry metaEntry = new ZipEntry("metadata.json");
            zos.putNextEntry(metaEntry);
            zos.write(metadata.getBytes());
            zos.closeEntry();

            // 添加技能文件
            if (version.getFiles() != null) {
                for (SkillVersion.FileInfo file : version.getFiles()) {
                    try {
                        byte[] content = storageService.readFileBytes(
                                skill.getId(), version.getId(), file.getPath());
                        ZipEntry entry = new ZipEntry(file.getPath());
                        zos.putNextEntry(entry);
                        zos.write(content);
                        zos.closeEntry();
                    } catch (Exception e) {
                        log.warn("Failed to add file to backup: {}", file.getPath(), e);
                    }
                }
            }
        }
        return baos.toByteArray();
    }

    private byte[] createSoulBackup(Soul soul) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("slug", soul.getSlug());
        data.put("displayName", soul.getDisplayName());
        data.put("description", soul.getDescription());
        data.put("createdAt", soul.getCreatedAt().toString());
        return objectMapper.writeValueAsBytes(data);
    }

    private String buildMetadataJson(Skill skill, SkillVersion version) throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("slug", skill.getSlug());
        metadata.put("displayName", skill.getDisplayName());
        metadata.put("version", version.getVersion());
        metadata.put("createdAt", version.getCreatedAt().toString());
        metadata.put("owner", skill.getOwner().getHandle());
        metadata.put("summary", skill.getSummary());
        metadata.put("parsedMetadata", version.getParsedMetadata());
        return objectMapper.writeValueAsString(metadata);
    }

    private Optional<String> getFileSha(String path) {
        try {
            var response = getClient().get()
                    .uri("/repos/{owner}/{repo}/contents/{path}",
                            getRepoOwner(), getRepoName(), path)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.containsKey("sha")) {
                return Optional.of((String) response.get("sha"));
            }
        } catch (Exception e) {
            // 文件不存在，返回空
        }
        return Optional.empty();
    }

    private String getRepoOwner() {
        String[] parts = backupRepo.split("/");
        return parts.length > 0 ? parts[0] : "";
    }

    private String getRepoName() {
        String[] parts = backupRepo.split("/");
        return parts.length > 1 ? parts[1] : "";
    }

    // Fallback methods
    private boolean backupFallback(Skill skill, SkillVersion version, User user, Exception ex) {
        log.warn("GitHub backup fallback triggered for skill {}: {}", skill.getSlug(), ex.getMessage());
        return false;
    }

    private Optional<byte[]> importFallback(String owner, String repo, String path, String ref, Exception ex) {
        log.warn("GitHub import fallback triggered: {}/{}/{}: {}", owner, repo, path, ex.getMessage());
        return Optional.empty();
    }
}
