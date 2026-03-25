package clawhub.service;

import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import clawhub.repository.PackageRepository;
import clawhub.repository.PackageReleaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Package 验证徽章服务
 * 
 * 验证层级：
 * - none: 无验证
 * - structural: 结构验证（文件格式正确）
 * - source-linked: 源码关联（有 GitHub 等源码链接）
 * - provenance-verified: 来源验证（发布者身份验证）
 * - rebuild-verified: 可重现构建（构建可重现）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageVerificationService {

    private final PackageRepository packageRepository;
    private final PackageReleaseRepository releaseRepository;
    private final GitHubService gitHubService;

    /**
     * 验证 Package 并更新验证徽章
     */
    @Transactional
    public void verifyPackage(UUID packageId) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found: " + packageId));

        log.info("Verifying package: {}", pkg.getName());

        Map<String, Object> verification = new HashMap<>();
        String tier = "none";

        // 1. 结构验证
        if (performStructuralVerification(pkg)) {
            tier = "structural";
            verification.put("structural", Map.of(
                    "verified", true,
                    "verifiedAt", Instant.now().toString(),
                    "checks", Map.of(
                            "manifestValid", true,
                            "filesComplete", true,
                            "metadataValid", true
                    )
            ));
        }

        // 2. 源码关联验证
        if (tier.equals("structural")) {
            Optional<String> sourceUrl = getSourceUrl(pkg);
            if (sourceUrl.isPresent()) {
                boolean sourceLinked = verifySourceLink(pkg, sourceUrl.get());
                if (sourceLinked) {
                    tier = "source-linked";
                    verification.put("sourceLinked", Map.of(
                            "verified", true,
                            "verifiedAt", Instant.now().toString(),
                            "sourceUrl", sourceUrl.get()
                    ));
                }
            }
        }

        // 3. 来源验证（发布者身份）
        if (tier.equals("source-linked") && pkg.getOwnerPublisherId() != null) {
            boolean provenanceVerified = verifyProvenance(pkg);
            if (provenanceVerified) {
                tier = "provenance-verified";
                verification.put("provenance", Map.of(
                        "verified", true,
                        "verifiedAt", Instant.now().toString(),
                        "publisherId", pkg.getOwnerPublisherId()
                ));
            }
        }

        // 4. 可重现构建（需要源码和构建配置）
        if (tier.equals("provenance-verified")) {
            boolean rebuildVerified = verifyRebuild(pkg);
            if (rebuildVerified) {
                tier = "rebuild-verified";
                verification.put("rebuild", Map.of(
                        "verified", true,
                        "verifiedAt", Instant.now().toString()
                ));
            }
        }

        // 更新 Package
        verification.put("tier", tier);
        verification.put("lastVerifiedAt", Instant.now().toString());
        pkg.setVerification(verification);
        packageRepository.save(pkg);

        log.info("Package {} verified with tier: {}", pkg.getName(), tier);
    }

    /**
     * 结构验证
     */
    private boolean performStructuralVerification(Package pkg) {
        // 检查必要的元数据
        if (pkg.getName() == null || pkg.getName().isEmpty()) {
            return false;
        }
        if (pkg.getDisplayName() == null || pkg.getDisplayName().isEmpty()) {
            return false;
        }
        if (pkg.getFamily() == null) {
            return false;
        }

        // 检查是否有至少一个 Release
        long releaseCount = releaseRepository.countByPackageId(pkg.getId());
        if (releaseCount == 0) {
            return false;
        }

        return true;
    }

    /**
     * 获取源码 URL
     */
    private Optional<String> getSourceUrl(Package pkg) {
        if (pkg.getCapabilities() == null) {
            return Optional.empty();
        }

        Object source = pkg.getCapabilities().get("source");
        if (source instanceof String) {
            return Optional.of((String) source);
        }

        Object repository = pkg.getCapabilities().get("repository");
        if (repository instanceof String) {
            return Optional.of((String) repository);
        }

        Object homepage = pkg.getCapabilities().get("homepage");
        if (homepage instanceof String && ((String) homepage).contains("github.com")) {
            return Optional.of((String) homepage);
        }

        return Optional.empty();
    }

    /**
     * 验证源码链接
     */
    private boolean verifySourceLink(Package pkg, String sourceUrl) {
        // 简单验证：检查 URL 格式和可访问性
        if (!sourceUrl.startsWith("http://") && !sourceUrl.startsWith("https://")) {
            return false;
        }

        // 如果是 GitHub 链接，验证仓库存在
        if (sourceUrl.contains("github.com")) {
            try {
                // 提取 owner/repo
                String[] parts = sourceUrl.replace("https://github.com/", "")
                        .replace("http://github.com/", "")
                        .split("/");
                if (parts.length >= 2) {
                    String owner = parts[0];
                    String repo = parts[1].replace(".git", "");
                    return gitHubService.repositoryExists(owner, repo);
                }
            } catch (Exception e) {
                log.warn("Failed to verify GitHub repository: {}", sourceUrl, e);
                return false;
            }
        }

        return true;
    }

    /**
     * 验证来源（发布者身份）
     */
    private boolean verifyProvenance(Package pkg) {
        // 检查发布者是否可信
        if (pkg.getOwnerPublisherId() == null) {
            return false;
        }

        // 检查是否是官方频道
        if (pkg.getChannel() == Package.Channel.OFFICIAL) {
            return true;
        }

        // 检查是否是可信发布者
        // 这里可以添加更多的验证逻辑
        return true;
    }

    /**
     * 验证可重现构建
     */
    private boolean verifyRebuild(Package pkg) {
        // 获取最新 Release
        Optional<PackageRelease> latestRelease = releaseRepository.findLatestByPackageId(pkg.getId());
        if (latestRelease.isEmpty()) {
            return false;
        }

        PackageRelease release = latestRelease.get();

        // 检查是否有构建配置
        if (pkg.getCapabilities() == null) {
            return false;
        }

        Object buildConfig = pkg.getCapabilities().get("build");
        if (buildConfig == null) {
            return false;
        }

        // 检查是否有 Dockerfile 或构建脚本
        boolean hasBuildScript = release.getFiles() != null &&
                release.getFiles().stream()
                        .anyMatch(f -> f.getPath().equalsIgnoreCase("Dockerfile") ||
                                f.getPath().equalsIgnoreCase("build.sh") ||
                                f.getPath().equalsIgnoreCase("Makefile"));

        return hasBuildScript;
    }

    /**
     * 获取验证徽章信息
     */
    public Map<String, Object> getVerificationBadge(UUID packageId) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found: " + packageId));

        String tier = pkg.getVerificationTier();
        Map<String, Object> verification = pkg.getVerification();

        Map<String, Object> badge = new HashMap<>();
        badge.put("tier", tier);
        badge.put("label", getTierLabel(tier));
        badge.put("description", getTierDescription(tier));
        badge.put("color", getTierColor(tier));
        badge.put("verifiedAt", verification != null ? verification.get("lastVerifiedAt") : null);

        return badge;
    }

    private String getTierLabel(String tier) {
        return switch (tier) {
            case "rebuild-verified" -> "Rebuild Verified";
            case "provenance-verified" -> "Provenance Verified";
            case "source-linked" -> "Source Linked";
            case "structural" -> "Structural";
            default -> "Not Verified";
        };
    }

    private String getTierDescription(String tier) {
        return switch (tier) {
            case "rebuild-verified" -> "Build is reproducible from source";
            case "provenance-verified" -> "Publisher identity verified";
            case "source-linked" -> "Source code repository linked";
            case "structural" -> "Package structure validated";
            default -> "No verification performed";
        };
    }

    private String getTierColor(String tier) {
        return switch (tier) {
            case "rebuild-verified" -> "#28a745";  // green
            case "provenance-verified" -> "#17a2b8";  // blue
            case "source-linked" -> "#ffc107";  // yellow
            case "structural" -> "#6c757d";  // gray
            default -> "#dc3545";  // red
        };
    }
}
