package clawhub.service;

import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VirusTotalServiceTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private PackageSecurityService packageSecurityService;

    private VirusTotalService virusTotalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 使用简化构造函数，实际应该注入 WebClient.Builder
        virusTotalService = new VirusTotalService(null, taskScheduler, packageSecurityService);
    }

    @Test
    void testBuildUndetectedFallbackAnalysis_success() {
        // 准备数据
        Package pkg = new Package();
        pkg.setId(java.util.UUID.randomUUID());
        pkg.setFamily(Package.Family.CODE_PLUGIN);

        PackageRelease release = new PackageRelease();
        release.setId(java.util.UUID.randomUUID());
        release.setPackage_(pkg);

        // 设置验证层级
        Map<String, Object> verification = new HashMap<>();
        verification.put("tier", "source-linked");
        release.setVerification(verification);

        // 设置LLM分析
        release.setLlmAnalysis(PackageRelease.LlmSecurityAnalysis.builder()
            .verdict("clean")
            .build());

        // 设置静态扫描
        release.setStaticScan(PackageRelease.StaticScanResult.builder()
            .status("clean")
            .build());

        // VT统计：无恶意，无可疑，有无害引擎
        VirusTotalService.VtAnalysisStats stats = new VirusTotalService.VtAnalysisStats(
            0,  // malicious
            0,  // suspicious
            5,  // harmless
            10  // undetected
        );

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildUndetectedFallbackAnalysis(release, pkg, stats);

        assertTrue(result.isPresent());
        assertEquals("clean", result.get().getStatus());
    }

    @Test
    void testBuildUndetectedFallbackAnalysis_skillPackageSkipped() {
        Package pkg = new Package();
        pkg.setFamily(Package.Family.SKILL); // skill 类型被跳过

        PackageRelease release = new PackageRelease();
        release.setPackage_(pkg);

        VirusTotalService.VtAnalysisStats stats = new VirusTotalService.VtAnalysisStats(0, 0, 5, 10);

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildUndetectedFallbackAnalysis(release, pkg, stats);

        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildUndetectedFallbackAnalysis_maliciousHits() {
        Package pkg = new Package();
        pkg.setFamily(Package.Family.CODE_PLUGIN);

        PackageRelease release = new PackageRelease();
        release.setPackage_(pkg);
        release.setVerification(Map.of("tier", "source-linked"));
        release.setLlmAnalysis(PackageRelease.LlmSecurityAnalysis.builder().verdict("clean").build());
        release.setStaticScan(PackageRelease.StaticScanResult.builder().status("clean").build());

        // VT统计：有恶意引擎
        VirusTotalService.VtAnalysisStats stats = new VirusTotalService.VtAnalysisStats(
            1,  // malicious - 有恶意命中
            0,  // suspicious
            5,  // harmless
            10  // undetected
        );

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildUndetectedFallbackAnalysis(release, pkg, stats);

        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildUndetectedFallbackAnalysis_untrustedTier() {
        Package pkg = new Package();
        pkg.setFamily(Package.Family.CODE_PLUGIN);

        PackageRelease release = new PackageRelease();
        release.setPackage_(pkg);

        // 非受信任的层级
        Map<String, Object> verification = new HashMap<>();
        verification.put("tier", "unknown-tier");
        release.setVerification(verification);

        release.setLlmAnalysis(PackageRelease.LlmSecurityAnalysis.builder().verdict("clean").build());
        release.setStaticScan(PackageRelease.StaticScanResult.builder().status("clean").build());

        VirusTotalService.VtAnalysisStats stats = new VirusTotalService.VtAnalysisStats(0, 0, 5, 10);

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildUndetectedFallbackAnalysis(release, pkg, stats);

        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildUndetectedFallbackAnalysis_llmNotClean() {
        Package pkg = new Package();
        pkg.setFamily(Package.Family.CODE_PLUGIN);

        PackageRelease release = new PackageRelease();
        release.setPackage_(pkg);
        release.setVerification(Map.of("tier", "source-linked"));

        // LLM 不是 clean
        release.setLlmAnalysis(PackageRelease.LlmSecurityAnalysis.builder()
            .verdict("suspicious")
            .build());

        release.setStaticScan(PackageRelease.StaticScanResult.builder().status("clean").build());

        VirusTotalService.VtAnalysisStats stats = new VirusTotalService.VtAnalysisStats(0, 0, 5, 10);

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildUndetectedFallbackAnalysis(release, pkg, stats);

        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildUndetectedFallbackAnalysis_staticMalicious() {
        Package pkg = new Package();
        pkg.setFamily(Package.Family.CODE_PLUGIN);

        PackageRelease release = new PackageRelease();
        release.setPackage_(pkg);
        release.setVerification(Map.of("tier", "source-linked"));
        release.setLlmAnalysis(PackageRelease.LlmSecurityAnalysis.builder().verdict("clean").build());

        // 静态扫描是 malicious
        release.setStaticScan(PackageRelease.StaticScanResult.builder()
            .status("malicious")
            .build());

        VirusTotalService.VtAnalysisStats stats = new VirusTotalService.VtAnalysisStats(0, 0, 5, 10);

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildUndetectedFallbackAnalysis(release, pkg, stats);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSchedulePackageScan() {
        UUID releaseId = java.util.UUID.randomUUID();
        UUID packageId = java.util.UUID.randomUUID();

        virusTotalService.schedulePackageScan(releaseId, packageId);

        // 验证调度器被调用
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void testBuildAnalysisFromVtResult_withAiResult() {
        // 模拟有AI分析结果的VT响应
        Map<String, Object> vtResponse = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();

        // AI结果
        java.util.List<Map<String, Object>> aiResults = new java.util.ArrayList<>();
        Map<String, Object> aiResult = new HashMap<>();
        aiResult.put("category", "code_insight");
        aiResult.put("verdict", "clean");
        aiResults.add(aiResult);
        attributes.put("crowdsourced_ai_results", aiResults);

        data.put("attributes", attributes);
        data.put("id", "test-id");
        vtResponse.put("data", data);

        Package pkg = new Package();
        PackageRelease release = new PackageRelease();

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildAnalysisFromVtResult(release, pkg, vtResponse);

        assertTrue(result.isPresent());
        assertEquals("clean", result.get().getStatus());
    }

    @Test
    void testBuildAnalysisFromVtResult_withStats() {
        // 模拟有引擎统计的VT响应
        Map<String, Object> vtResponse = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();

        // 引擎统计
        Map<String, Object> stats = new HashMap<>();
        stats.put("malicious", 0);
        stats.put("suspicious", 0);
        stats.put("harmless", 10);
        stats.put("undetected", 5);
        attributes.put("last_analysis_stats", stats);

        data.put("attributes", attributes);
        data.put("id", "test-id");
        vtResponse.put("data", data);

        Package pkg = new Package();
        pkg.setFamily(Package.Family.CODE_PLUGIN);

        PackageRelease release = new PackageRelease();
        release.setPackage_(pkg);
        release.setVerification(Map.of("tier", "source-linked"));
        release.setLlmAnalysis(PackageRelease.LlmSecurityAnalysis.builder().verdict("clean").build());
        release.setStaticScan(PackageRelease.StaticScanResult.builder().status("clean").build());

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildAnalysisFromVtResult(release, pkg, vtResponse);

        assertTrue(result.isPresent());
        assertEquals("clean", result.get().getStatus());
    }

    @Test
    void testBuildAnalysisFromVtResult_maliciousStats() {
        Map<String, Object> vtResponse = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();

        // 有恶意引擎
        Map<String, Object> stats = new HashMap<>();
        stats.put("malicious", 2);
        stats.put("suspicious", 1);
        stats.put("harmless", 5);
        stats.put("undetected", 3);
        attributes.put("last_analysis_stats", stats);

        data.put("attributes", attributes);
        data.put("id", "test-id");
        vtResponse.put("data", data);

        Package pkg = new Package();
        PackageRelease release = new PackageRelease();

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildAnalysisFromVtResult(release, pkg, vtResponse);

        assertTrue(result.isPresent());
        assertEquals("malicious", result.get().getStatus());
    }

    @Test
    void testBuildAnalysisFromVtResult_nullResponse() {
        Package pkg = new Package();
        PackageRelease release = new PackageRelease();

        Optional<PackageRelease.VirusTotalAnalysis> result =
            virusTotalService.buildAnalysisFromVtResult(release, pkg, null);

        assertTrue(result.isEmpty());
    }
}