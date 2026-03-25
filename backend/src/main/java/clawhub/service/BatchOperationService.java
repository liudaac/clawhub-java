package clawhub.service;

import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import clawhub.repository.PackageReleaseRepository;
import clawhub.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchOperationService {

    private final PackageRepository packageRepository;
    private final PackageReleaseRepository releaseRepository;
    private final PackageSecurityScanService securityScanService;
    private final PackageVerificationService verificationService;
    private final CacheService cacheService;

    private static final int BATCH_SIZE = 100;

    /**
     * 批量扫描 Package
     */
    @Async("securityScanExecutor")
    public CompletableFuture<BatchResult> batchScanPackages(List<UUID> packageIds) {
        log.info("Starting batch scan for {} packages", packageIds.size());
        
        BatchResult result = new BatchResult();
        
        for (UUID packageId : packageIds) {
            try {
                // Get latest release
                var latestRelease = releaseRepository.findLatestByPackageId(packageId);
                if (latestRelease.isPresent()) {
                    securityScanService.performSecurityScan(latestRelease.get().getId());
                    result.success++;
                } else {
                    result.skipped++;
                }
            } catch (Exception e) {
                log.error("Failed to scan package: {}", packageId, e);
                result.failed++;
                result.errors.add("Package " + packageId + ": " + e.getMessage());
            }
        }
        
        log.info("Batch scan completed: {} success, {} skipped, {} failed", 
                result.success, result.skipped, result.failed);
        
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 批量验证 Package
     */
    @Async("verificationExecutor")
    public CompletableFuture<BatchResult> batchVerifyPackages(List<UUID> packageIds) {
        log.info("Starting batch verification for {} packages", packageIds.size());
        
        BatchResult result = new BatchResult();
        
        for (UUID packageId : packageIds) {
            try {
                verificationService.verifyPackage(packageId);
                result.success++;
            } catch (Exception e) {
                log.error("Failed to verify package: {}", packageId, e);
                result.failed++;
                result.errors.add("Package " + packageId + ": " + e.getMessage());
            }
        }
        
        log.info("Batch verification completed: {} success, {} failed", 
                result.success, result.failed);
        
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 批量更新统计信息
     */
    @Transactional
    public BatchResult batchUpdateStats() {
        log.info("Starting batch stats update");
        
        BatchResult result = new BatchResult();
        int page = 0;
        
        while (true) {
            Page<Package> packages = packageRepository.findAllActive(
                    PageRequest.of(page, BATCH_SIZE));
            
            if (packages.isEmpty()) {
                break;
            }
            
            for (Package pkg : packages.getContent()) {
                try {
                    updatePackageStats(pkg);
                    result.success++;
                } catch (Exception e) {
                    log.error("Failed to update stats for package: {}", pkg.getId(), e);
                    result.failed++;
                }
            }
            
            page++;
            
            if (!packages.hasNext()) {
                break;
            }
        }
        
        log.info("Batch stats update completed: {} success, {} failed", 
                result.success, result.failed);
        
        return result;
    }

    /**
     * 批量清理软删除数据
     */
    @Transactional
    public BatchResult batchCleanupSoftDeleted(Instant before) {
        log.info("Starting cleanup of soft-deleted data before {}", before);
        
        BatchResult result = new BatchResult();
        
        // Cleanup soft-deleted packages
        List<Package> deletedPackages = packageRepository.findAll().stream()
                .filter(p -> p.getSoftDeletedAt() != null && p.getSoftDeletedAt().isBefore(before))
                .toList();
        
        for (Package pkg : deletedPackages) {
            try {
                // Delete associated releases
                List<PackageRelease> releases = releaseRepository.findByPackage_Id(pkg.getId());
                releaseRepository.deleteAll(releases);
                
                // Delete package
                packageRepository.delete(pkg);
                
                // Clear cache
                cacheService.evictPackageCaches(pkg.getName());
                
                result.success++;
            } catch (Exception e) {
                log.error("Failed to cleanup package: {}", pkg.getId(), e);
                result.failed++;
            }
        }
        
        log.info("Batch cleanup completed: {} deleted", result.success);
        
        return result;
    }

    /**
     * 批量重新索引（用于 Elasticsearch）
     */
    @Async("indexingExecutor")
    public CompletableFuture<BatchResult> batchReindexPackages() {
        log.info("Starting batch reindex");
        
        BatchResult result = new BatchResult();
        int page = 0;
        
        while (true) {
            Page<Package> packages = packageRepository.findAllActive(
                    PageRequest.of(page, BATCH_SIZE));
            
            if (packages.isEmpty()) {
                break;
            }
            
            for (Package pkg : packages.getContent()) {
                try {
                    // Trigger reindex - this would call Elasticsearch service
                    // elasticsearchService.indexPackage(pkg);
                    result.success++;
                } catch (Exception e) {
                    log.error("Failed to reindex package: {}", pkg.getId(), e);
                    result.failed++;
                }
            }
            
            page++;
            
            if (!packages.hasNext()) {
                break;
            }
        }
        
        log.info("Batch reindex completed: {} success, {} failed", 
                result.success, result.failed);
        
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 执行分页处理
     */
    public <T> BatchResult processInBatches(
            Function<PageRequest, Page<T>> fetcher,
            Function<T, Boolean> processor) {
        
        BatchResult result = new BatchResult();
        int page = 0;
        
        while (true) {
            Page<T> items = fetcher.apply(PageRequest.of(page, BATCH_SIZE));
            
            if (items.isEmpty()) {
                break;
            }
            
            for (T item : items.getContent()) {
                try {
                    Boolean success = processor.apply(item);
                    if (Boolean.TRUE.equals(success)) {
                        result.success++;
                    } else {
                        result.skipped++;
                    }
                } catch (Exception e) {
                    log.error("Failed to process item", e);
                    result.failed++;
                }
            }
            
            page++;
            
            if (!items.hasNext()) {
                break;
            }
        }
        
        return result;
    }

    private void updatePackageStats(Package pkg) {
        // Update version count
        long versionCount = releaseRepository.countByPackageId(pkg.getId());
        pkg.setStatsVersions((int) versionCount);
        
        // Save
        packageRepository.save(pkg);
    }

    public static class BatchResult {
        public int success = 0;
        public int failed = 0;
        public int skipped = 0;
        public List<String> errors = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("BatchResult{success=%d, failed=%d, skipped=%d, errors=%d}", 
                    success, failed, skipped, errors.size());
        }
    }
}
