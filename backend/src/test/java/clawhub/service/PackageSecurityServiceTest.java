package clawhub.service;

import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PackageSecurityServiceTest {

    private PackageSecurityService service;

    @BeforeEach
    void setUp() {
        service = new PackageSecurityService();
    }

    @Test
    void testNormalizeScanStatus() {
        assertEquals(PackageSecurityService.ScanStatus.CLEAN, service.normalizeScanStatus("clean"));
        assertEquals(PackageSecurityService.ScanStatus.CLEAN, service.normalizeScanStatus("CLEAN"));
        assertEquals(PackageSecurityService.ScanStatus.MALICIOUS, service.normalizeScanStatus("malicious"));
        assertEquals(PackageSecurityService.ScanStatus.PENDING, service.normalizeScanStatus("pending"));
        assertEquals(PackageSecurityService.ScanStatus.NOT_RUN, service.normalizeScanStatus("not-run"));
        assertEquals(PackageSecurityService.ScanStatus.NOT_RUN, service.normalizeScanStatus("not_run"));
        assertNull(service.normalizeScanStatus("unknown"));
        assertNull(service.normalizeScanStatus(null));
    }

    @Test
    void testResolvePackageReleaseScanStatus_staticMalicious() {
        PackageRelease release = new PackageRelease();
        release.setStaticScan(PackageRelease.StaticScanResult.builder()
                .status("malicious")
                .build());

        PackageSecurityService.ScanStatus status = service.resolvePackageReleaseScanStatus(release);
        assertEquals(PackageSecurityService.ScanStatus.MALICIOUS, status);
    }

    @Test
    void testResolvePackageReleaseScanStatus_vtMalicious() {
        PackageRelease release = new PackageRelease();
        release.setStaticScan(PackageRelease.StaticScanResult.builder()
                .status("clean")
                .build());
        release.setVtAnalysis(PackageRelease.VirusTotalAnalysis.builder()
                .status("malicious")
                .build());

        PackageSecurityService.ScanStatus status = service.resolvePackageReleaseScanStatus(release);
        assertEquals(PackageSecurityService.ScanStatus.MALICIOUS, status);
    }

    @Test
    void testResolvePackageReleaseScanStatus_vtClean() {
        PackageRelease release = new PackageRelease();
        release.setVtAnalysis(PackageRelease.VirusTotalAnalysis.builder()
                .status("clean")
                .build());

        PackageSecurityService.ScanStatus status = service.resolvePackageReleaseScanStatus(release);
        assertEquals(PackageSecurityService.ScanStatus.CLEAN, status);
    }

    @Test
    void testResolvePackageReleaseScanStatus_pendingWithHash() {
        PackageRelease release = new PackageRelease();
        release.setIntegritySha256("abc123");

        PackageSecurityService.ScanStatus status = service.resolvePackageReleaseScanStatus(release);
        assertEquals(PackageSecurityService.ScanStatus.PENDING, status);
    }

    @Test
    void testResolvePackageReleaseScanStatus_notRun() {
        PackageRelease release = new PackageRelease();

        PackageSecurityService.ScanStatus status = service.resolvePackageReleaseScanStatus(release);
        assertEquals(PackageSecurityService.ScanStatus.NOT_RUN, status);
    }

    @Test
    void testIsPackageBlockedFromPublic_onlyMalicious() {
        // 关键变更：仅阻止 MALICIOUS
        assertTrue(service.isPackageBlockedFromPublic(PackageSecurityService.ScanStatus.MALICIOUS));
        assertFalse(service.isPackageBlockedFromPublic(PackageSecurityService.ScanStatus.PENDING));
        assertFalse(service.isPackageBlockedFromPublic(PackageSecurityService.ScanStatus.SUSPICIOUS));
        assertFalse(service.isPackageBlockedFromPublic(PackageSecurityService.ScanStatus.CLEAN));
        assertFalse(service.isPackageBlockedFromPublic(PackageSecurityService.ScanStatus.NOT_RUN));
    }

    @Test
    void testGetPackageDownloadSecurityBlock_malicious() {
        PackageRelease release = new PackageRelease();
        release.setVtAnalysis(PackageRelease.VirusTotalAnalysis.builder()
                .status("malicious")
                .build());

        PackageSecurityService.DownloadSecurityBlock block = service.getPackageDownloadSecurityBlock(release);
        assertNotNull(block);
        assertEquals(403, block.status());
        assertTrue(block.message().contains("malicious"));
    }

    @Test
    void testGetPackageDownloadSecurityBlock_pendingAllowed() {
        // PENDING 状态的包现在应该可以下载
        PackageRelease release = new PackageRelease();
        release.setIntegritySha256("abc123");

        PackageSecurityService.DownloadSecurityBlock block = service.getPackageDownloadSecurityBlock(release);
        assertNull(block);
    }

    @Test
    void testGetPackageDownloadSecurityBlock_cleanAllowed() {
        PackageRelease release = new PackageRelease();
        release.setVtAnalysis(PackageRelease.VirusTotalAnalysis.builder()
                .status("clean")
                .build());

        PackageSecurityService.DownloadSecurityBlock block = service.getPackageDownloadSecurityBlock(release);
        assertNull(block);
    }

    @Test
    void testSyncLatestPackageVerification() {
        Package pkg = new Package();
        pkg.setId(java.util.UUID.randomUUID());
        pkg.setName("test-package");

        PackageRelease release = new PackageRelease();
        release.setId(java.util.UUID.randomUUID());
        release.setPackage_(pkg);
        release.setVtAnalysis(PackageRelease.VirusTotalAnalysis.builder()
                .status("clean")
                .build());

        pkg.setLatestReleaseId(release.getId());
        pkg.setScanStatus(Package.ScanStatus.NOT_RUN);

        boolean synced = service.syncLatestPackageVerification(release);
        assertTrue(synced);
        assertEquals(Package.ScanStatus.CLEAN, pkg.getScanStatus());
    }

    @Test
    void testSyncLatestPackageVerification_notLatest() {
        Package pkg = new Package();
        pkg.setId(java.util.UUID.randomUUID());
        pkg.setLatestReleaseId(java.util.UUID.randomUUID()); // 不同的ID

        PackageRelease release = new PackageRelease();
        release.setId(java.util.UUID.randomUUID());
        release.setPackage_(pkg);

        boolean synced = service.syncLatestPackageVerification(release);
        assertFalse(synced);
    }
}
