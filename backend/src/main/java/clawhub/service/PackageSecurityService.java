package clawhub.service;

import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 包安全扫描服务
 * 集中化处理包安全扫描状态的解析和裁决
 *
 * 对应原版: convex/lib/packageSecurity.ts
 */
@Slf4j
@Service
public class PackageSecurityService {

    /**
     * 扫描状态枚举
     */
    public enum ScanStatus {
        CLEAN, SUSPICIOUS, MALICIOUS, PENDING, NOT_RUN
    }

    /**
     * 标准化扫描状态字符串
     * 对应原版: normalizePackageScanStatus()
     *
     * @param status 状态字符串
     * @return 标准化的 ScanStatus，无法识别时返回 null
     */
    public ScanStatus normalizeScanStatus(String status) {
        if (status == null) return null;

        String normalized = status.trim().toLowerCase();
        return switch (normalized) {
            case "clean" -> ScanStatus.CLEAN;
            case "suspicious" -> ScanStatus.SUSPICIOUS;
            case "malicious" -> ScanStatus.MALICIOUS;
            case "pending" -> ScanStatus.PENDING;
            case "not-run", "not_run" -> ScanStatus.NOT_RUN;
            default -> null;
        };
    }

    /**
     * 解析包发布的综合扫描状态
     * 对应原版: resolvePackageReleaseScanStatus()
     *
     * 优先级规则（高到低）：
     * 1. 静态扫描 malicious
     * 2. VT扫描 malicious
     * 3. 验证状态 malicious
     * 4. VT扫描其他状态
     * 5. 验证状态（非 NOT_RUN）
     * 6. 有 sha256hash 则 PENDING
     * 7. 验证状态或 NOT_RUN
     *
     * @param release 包发布版本
     * @return 综合扫描状态
     */
    public ScanStatus resolvePackageReleaseScanStatus(PackageRelease release) {
        if (release == null) {
            return ScanStatus.NOT_RUN;
        }

        // 1. 检查静态扫描
        if (release.getStaticScan() != null) {
            ScanStatus staticStatus = normalizeScanStatus(release.getStaticScan().getStatus());
            if (staticStatus == ScanStatus.MALICIOUS) {
                log.debug("Scan status resolved to MALICIOUS from static scan");
                return ScanStatus.MALICIOUS;
            }
        }

        // 2. 检查VT扫描
        if (release.getVtAnalysis() != null) {
            ScanStatus vtStatus = normalizeScanStatus(release.getVtAnalysis().getStatus());
            if (vtStatus == ScanStatus.MALICIOUS) {
                log.debug("Scan status resolved to MALICIOUS from VT analysis");
                return ScanStatus.MALICIOUS;
            }
            // VT有明确状态时直接返回
            if (vtStatus != null) {
                log.debug("Scan status resolved to {} from VT analysis", vtStatus);
                return vtStatus;
            }
        }

        // 3. 检查验证状态
        if (release.getVerification() != null) {
            Object scanStatusObj = release.getVerification().get("scanStatus");
            if (scanStatusObj != null) {
                ScanStatus verificationStatus = normalizeScanStatus(scanStatusObj.toString());
                if (verificationStatus == ScanStatus.MALICIOUS) {
                    log.debug("Scan status resolved to MALICIOUS from verification");
                    return ScanStatus.MALICIOUS;
                }
                // 验证状态非 NOT_RUN 时返回
                if (verificationStatus != null && verificationStatus != ScanStatus.NOT_RUN) {
                    log.debug("Scan status resolved to {} from verification", verificationStatus);
                    return verificationStatus;
                }
            }
        }

        // 4. 如果有 sha256hash，说明正在等待扫描
        if (release.getIntegritySha256() != null && !release.getIntegritySha256().isEmpty()) {
            log.debug("Scan status resolved to PENDING (has sha256hash)");
            return ScanStatus.PENDING;
        }

        // 5. 默认返回验证状态或 NOT_RUN
        if (release.getVerification() != null) {
            Object scanStatusObj = release.getVerification().get("scanStatus");
            if (scanStatusObj != null) {
                ScanStatus v = normalizeScanStatus(scanStatusObj.toString());
                return v != null ? v : ScanStatus.NOT_RUN;
            }
        }

        return ScanStatus.NOT_RUN;
    }

    /**
     * 检查包是否被阻止公开访问
     * 对应原版: isPackageBlockedFromPublic()
     *
     * 关键变更：原版从阻止 PENDING 改为仅阻止 MALICIOUS
     *
     * @param scanStatus 扫描状态
     * @return true 如果被阻止
     */
    public boolean isPackageBlockedFromPublic(ScanStatus scanStatus) {
        // 原版变更：仅阻止 MALICIOUS（不再阻止 PENDING）
        return scanStatus == ScanStatus.MALICIOUS;
    }

    /**
     * 检查包是否被阻止公开访问（重载）
     *
     * @param release 包发布版本
     * @return true 如果被阻止
     */
    public boolean isPackageBlockedFromPublic(PackageRelease release) {
        ScanStatus scanStatus = resolvePackageReleaseScanStatus(release);
        return isPackageBlockedFromPublic(scanStatus);
    }

    /**
     * 获取包下载安全拦截信息
     * 对应原版: getPackageDownloadSecurityBlock()
     *
     * @param release 包发布版本
     * @return 拦截信息，null 表示允许下载
     */
    public DownloadSecurityBlock getPackageDownloadSecurityBlock(PackageRelease release) {
        ScanStatus scanStatus = resolvePackageReleaseScanStatus(release);

        if (scanStatus == ScanStatus.MALICIOUS) {
            return new DownloadSecurityBlock(
                403,
                "Blocked: this package release has been flagged as malicious and cannot be downloaded."
            );
        }

        return null;
    }

    /**
     * 同步最新版本的验证状态到包
     * 对应原版: syncLatestPackageVerification()
     *
     * @param release 包发布版本
     * @return 是否同步成功
     */
    public boolean syncLatestPackageVerification(PackageRelease release) {
        if (release == null || release.getPackage_() == null) {
            return false;
        }

        Package pkg = release.getPackage_();

        // 检查是否是最新版本
        if (pkg.getLatestReleaseId() == null ||
            !pkg.getLatestReleaseId().equals(release.getId())) {
            return false;
        }

        // 解析当前扫描状态
        ScanStatus scanStatus = resolvePackageReleaseScanStatus(release);

        // 更新发布版本的验证状态
        if (release.getVerification() != null) {
            release.getVerification().put("scanStatus", scanStatus.name().toLowerCase());
        }

        // 更新包的扫描状态
        pkg.setScanStatus(mapToEntityScanStatus(scanStatus));

        log.info("Synced scan status for package {}: {}", pkg.getName(), scanStatus);
        return true;
    }

    /**
     * 映射服务层 ScanStatus 到实体层 ScanStatus
     */
    private Package.ScanStatus mapToEntityScanStatus(ScanStatus status) {
        return switch (status) {
            case CLEAN -> Package.ScanStatus.CLEAN;
            case SUSPICIOUS -> Package.ScanStatus.SUSPICIOUS;
            case MALICIOUS -> Package.ScanStatus.MALICIOUS;
            case PENDING -> Package.ScanStatus.PENDING;
            case NOT_RUN -> Package.ScanStatus.NOT_RUN;
        };
    }

    /**
     * 下载安全拦截信息
     */
    public record DownloadSecurityBlock(int status, String message) {
        /**
         * 转换为响应体
         */
        public Map<String, String> toResponseBody() {
            return Map.of("error", message);
        }
    }
}
