package clawhub.service;

import clawhub.entity.PackageRelease;
import clawhub.entity.SkillTransfer;
import clawhub.repository.PackageReleaseRepository;
import clawhub.repository.SkillRepository;
import clawhub.repository.SkillTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final SkillTransferRepository transferRepository;
    private final SkillRepository skillRepository;
    private final SecurityScanService securityScanService;
    private final GitHubService gitHubService;
    private final SkillVersionService skillVersionService;
    private final CommentModerationService commentModerationService;
    private final PackageReleaseRepository packageReleaseRepository;
    private final PackageSecurityScanService packageSecurityScanService;

    /**
     * 清理过期的技能转移请求
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    @Transactional
    public void expireOldTransfers() {
        log.debug("Running expire old transfers task");
        List<SkillTransfer> expired = transferRepository.findExpired(Instant.now());
        int count = 0;
        for (SkillTransfer transfer : expired) {
            if (transfer.getStatus() == SkillTransfer.TransferStatus.PENDING) {
                transfer.setStatus(SkillTransfer.TransferStatus.EXPIRED);
                transferRepository.save(transfer);
                count++;
            }
        }
        if (count > 0) {
            log.info("Expired {} old transfer requests", count);
        }
    }

    /**
     * 处理待扫描的技能版本
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60 * 1000)
    @Transactional
    public void processPendingScans() {
        log.debug("Running pending scans task");
        var pendingVersions = skillVersionService.findPendingScans(PageRequest.of(0, 10));
        for (var version : pendingVersions) {
            try {
                securityScanService.performSecurityScan(version.getId());
            } catch (Exception e) {
                log.error("Failed to scan version: {}", version.getId(), e);
            }
        }
    }

    /**
     * 备份技能到 GitHub
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void backupSkillsToGitHub() {
        log.info("Running GitHub backup task");
        var skills = skillRepository.findByModerationStatus(
                Skill.ModerationStatus.ACTIVE,
                PageRequest.of(0, 100));

        for (Skill skill : skills) {
            try {
                var latestVersion = skill.getLatestVersion();
                if (latestVersion != null) {
                    gitHubService.backupSkill(skill, latestVersion, skill.getOwner());
                }
            } catch (Exception e) {
                log.error("Failed to backup skill: {}", skill.getSlug(), e);
            }
        }
    }

    /**
     * 更新技能统计信息
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void updateSkillStats() {
        log.debug("Running update skill stats task");
        skillVersionService.processStatEvents();
    }

    /**
     * 清理旧的下载记录（去重）
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldDownloads() {
        log.info("Running cleanup old downloads task");
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        skillVersionService.cleanupOldDownloads(cutoff);
    }

    /**
     * 重新计算热门技能排行
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void rebuildTrendingSkills() {
        log.info("Running rebuild trending skills task");
        skillVersionService.rebuildTrendingSkills();
    }

    /**
     * 处理待处理的评论举报
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void processPendingCommentReports() {
        log.debug("Running process pending comment reports task");
        var pendingReports = commentModerationService.getPendingReports(
                PageRequest.of(0, 50));
        // 这里可以添加自动处理逻辑
    }

    /**
     * 同步全局统计
     * 每15分钟执行一次
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Transactional(readOnly = true)
    public void syncGlobalStats() {
        log.debug("Running sync global stats task");
        skillVersionService.syncGlobalStats();
    }

    /**
     * 健康检查任务
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void healthCheck() {
        // 简单的健康检查，记录系统状态
        log.debug("Health check passed");
    }

    // ==================== Package Scan Backfill Tasks ====================

    /**
     * 包发布版本安全扫描回扫任务
     * 每30秒执行一次
     * 对应原版: getPackageReleaseScanBackfillBatchInternal
     */
    @Scheduled(fixedRate = 30 * 1000)
    @Transactional
    public void processPackageScanBackfill() {
        processPackageScanBackfillInternal(50, true); // 默认优先扫描最近发布的
    }

    /**
     * 获取待回扫的发布版本批次
     * 优先扫描最近发布的，再处理积压的
     *
     * @param batchSize 批次大小
     * @param prioritizeRecent 是否优先扫描最近发布的
     */
    @Transactional(readOnly = true)
    public List<PackageRelease> getBackfillBatch(int batchSize, boolean prioritizeRecent) {
        int actualBatchSize = Math.max(1, Math.min(batchSize, 200));

        if (prioritizeRecent) {
            // 优先模式：先取最近的，再取积压的
            List<PackageRelease> recent = packageReleaseRepository
                .findTopNByOrderByCreatedAtDesc(PageRequest.of(0, actualBatchSize * 2));

            List<PackageRelease> backlog = packageReleaseRepository
                .findByScanStatusPendingOrderByCreatedAtAsc(PageRequest.of(0, actualBatchSize * 3));

            // 合并并去重
            java.util.Set<UUID> seen = new java.util.HashSet<>();
            List<PackageRelease> result = new java.util.ArrayList<>();

            // 先添加最近的
            for (PackageRelease r : recent) {
                if (seen.add(r.getId())) {
                    result.add(r);
                }
            }

            // 再添加积压的
            for (PackageRelease r : backlog) {
                if (seen.add(r.getId())) {
                    result.add(r);
                }
            }

            return result.stream().limit(actualBatchSize).toList();
        } else {
            // 普通模式：按创建时间顺序
            return packageReleaseRepository
                .findByScanStatusPendingOrderByCreatedAtAsc(PageRequest.of(0, actualBatchSize));
        }
    }

    /**
     * 处理包扫描回扫
     *
     * @param batchSize 批次大小
     * @param prioritizeRecent 是否优先扫描最近发布的
     */
    @Transactional
    public void processPackageScanBackfillInternal(int batchSize, boolean prioritizeRecent) {
        log.debug("Running package scan backfill task (prioritizeRecent={})", prioritizeRecent);

        List<PackageRelease> batch = getBackfillBatch(batchSize, prioritizeRecent);

        if (batch.isEmpty()) {
            log.debug("No packages to backfill");
            return;
        }

        log.info("Processing {} packages for scan backfill", batch.size());

        int successCount = 0;
        int failCount = 0;

        for (PackageRelease release : batch) {
            try {
                // 检查是否已删除
                if (release.getSoftDeletedAt() != null) {
                    continue;
                }

                // 检查是否已扫描
                if (release.getVtAnalysis() != null && release.getLlmAnalysis() != null) {
                    continue;
                }

                // 执行扫描
                packageSecurityScanService.performSecurityScan(release.getId());
                successCount++;

                // 避免过快执行
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Failed to backfill scan for release: {}", release.getId(), e);
                failCount++;
            }
        }

        log.info("Package scan backfill completed: {} success, {} failed", successCount, failCount);
    }
}
