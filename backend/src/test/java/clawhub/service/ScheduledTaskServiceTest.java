package clawhub.service;

import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import clawhub.repository.PackageReleaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScheduledTaskServiceTest {

    @Mock
    private PackageReleaseRepository packageReleaseRepository;

    @Mock
    private PackageSecurityScanService packageSecurityScanService;

    private ScheduledTaskService scheduledTaskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 简化构造函数，实际应该注入所有依赖
        scheduledTaskService = new ScheduledTaskService(
            null, null, null, null, null, null,
            packageReleaseRepository, packageSecurityScanService
        );
    }

    @Test
    void testGetBackfillBatch_prioritizeRecent() {
        // 准备数据
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        PackageRelease recent1 = createRelease(id1, Instant.now());
        PackageRelease recent2 = createRelease(id2, Instant.now().minusSeconds(60));
        PackageRelease backlog1 = createRelease(id3, Instant.now().minusSeconds(3600));

        List<PackageRelease> recentList = List.of(recent1, recent2);
        List<PackageRelease> backlogList = List.of(backlog1);

        when(packageReleaseRepository.findTopNByOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(recentList);
        when(packageReleaseRepository.findByScanStatusPendingOrderByCreatedAtAsc(any(Pageable.class)))
            .thenReturn(backlogList);

        // 执行
        List<PackageRelease> result = scheduledTaskService.getBackfillBatch(10, true);

        // 验证
        assertEquals(3, result.size());
        // 最近的应该在前
        assertEquals(id1, result.get(0).getId());
        assertEquals(id2, result.get(1).getId());
        assertEquals(id3, result.get(2).getId());
    }

    @Test
    void testGetBackfillBatch_prioritizeRecent_deduplication() {
        // 准备数据 - 有重复
        UUID id1 = UUID.randomUUID();

        PackageRelease release = createRelease(id1, Instant.now());

        // recent 和 backlog 都包含同一个 release
        List<PackageRelease> recentList = List.of(release);
        List<PackageRelease> backlogList = List.of(release);

        when(packageReleaseRepository.findTopNByOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(recentList);
        when(packageReleaseRepository.findByScanStatusPendingOrderByCreatedAtAsc(any(Pageable.class)))
            .thenReturn(backlogList);

        // 执行
        List<PackageRelease> result = scheduledTaskService.getBackfillBatch(10, true);

        // 验证 - 应该去重
        assertEquals(1, result.size());
        assertEquals(id1, result.get(0).getId());
    }

    @Test
    void testGetBackfillBatch_notPrioritizeRecent() {
        // 准备数据
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        PackageRelease release1 = createRelease(id1, Instant.now().minusSeconds(3600));
        PackageRelease release2 = createRelease(id2, Instant.now().minusSeconds(1800));

        List<PackageRelease> backlogList = List.of(release1, release2);

        when(packageReleaseRepository.findByScanStatusPendingOrderByCreatedAtAsc(any(Pageable.class)))
            .thenReturn(backlogList);

        // 执行
        List<PackageRelease> result = scheduledTaskService.getBackfillBatch(10, false);

        // 验证 - 按创建时间升序
        assertEquals(2, result.size());
        assertEquals(id1, result.get(0).getId()); // 较早的在前
        assertEquals(id2, result.get(1).getId());
    }

    @Test
    void testGetBackfillBatch_batchSizeLimit() {
        // 准备数据 - 超过批次大小
        List<PackageRelease> recentList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            recentList.add(createRelease(UUID.randomUUID(), Instant.now().minusSeconds(i * 60)));
        }

        when(packageReleaseRepository.findTopNByOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(recentList);
        when(packageReleaseRepository.findByScanStatusPendingOrderByCreatedAtAsc(any(Pageable.class)))
            .thenReturn(List.of());

        // 执行 - 限制批次大小为5
        List<PackageRelease> result = scheduledTaskService.getBackfillBatch(5, true);

        // 验证
        assertEquals(5, result.size());
    }

    @Test
    void testGetBackfillBatch_batchSizeBounds() {
        // 测试批次大小边界
        when(packageReleaseRepository.findTopNByOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(List.of());
        when(packageReleaseRepository.findByScanStatusPendingOrderByCreatedAtAsc(any(Pageable.class)))
            .thenReturn(List.of());

        // 批次大小为0应该调整为1
        List<PackageRelease> result = scheduledTaskService.getBackfillBatch(0, true);
        assertTrue(result.isEmpty());

        // 批次大小超过200应该限制为200
        // 这里只是验证不抛出异常
        result = scheduledTaskService.getBackfillBatch(1000, true);
        assertTrue(result.isEmpty());
    }

    @Test
    void testProcessPackageScanBackfillInternal_emptyBatch() {
        when(packageReleaseRepository.findTopNByOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(List.of());
        when(packageReleaseRepository.findByScanStatusPendingOrderByCreatedAtAsc(any(Pageable.class)))
            .thenReturn(List.of());

        // 执行
        scheduledTaskService.processPackageScanBackfillInternal(10, true);

        // 验证 - 没有调用扫描服务
        verify(packageSecurityScanService, never()).performSecurityScan(any());
    }

    @Test
    void testProcessPackageScanBackfillInternal_skipDeleted() {
        // 准备数据 - 包含已删除的
        UUID id1 = UUID.randomUUID();
        PackageRelease deletedRelease = createRelease(id1, Instant.now());
        deletedRelease.setSoftDeletedAt(Instant.now()); // 已删除

        when(packageReleaseRepository.findTopNByOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(List.of(deletedRelease));
        when(packageReleaseRepository.findByScanStatusPendingOrderByCreatedAtAsc(any(Pageable.class)))
            .thenReturn(List.of());

        // 执行
        scheduledTaskService.processPackageScanBackfillInternal(10, true);

        // 验证 - 已删除的应该被跳过
        verify(packageSecurityScanService, never()).performSecurityScan(any());
    }

    @Test
    void testProcessPackageScanBackfillInternal_skipAlreadyScanned() {
        // 准备数据 - 已扫描的
        UUID id1 = UUID.randomUUID();
        PackageRelease scannedRelease = createRelease(id1, Instant.now());
        scannedRelease.setVtAnalysis(PackageRelease.VirusTotalAnalysis.builder().build());
        scannedRelease.setLlmAnalysis(PackageRelease.LlmSecurityAnalysis.builder().build());

        when(packageReleaseRepository.findTopNByOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(List.of(scannedRelease));
        when(packageReleaseRepository.findByScanStatusPendingOrderByCreatedAtAsc(any(Pageable.class)))
            .thenReturn(List.of());

        // 执行
        scheduledTaskService.processPackageScanBackfillInternal(10, true);

        // 验证 - 已扫描的应该被跳过
        verify(packageSecurityScanService, never()).performSecurityScan(any());
    }

    private PackageRelease createRelease(UUID id, Instant createdAt) {
        Package pkg = new Package();
        pkg.setId(UUID.randomUUID());
        pkg.setName("test-package");

        PackageRelease release = new PackageRelease();
        release.setId(id);
        release.setPackage_(pkg);
        release.setCreatedAt(createdAt);
        release.setVersion("1.0.0");
        return release;
    }
}
