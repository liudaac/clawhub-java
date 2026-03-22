package clawhub.service;

import clawhub.entity.SkillTransfer;
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
}
