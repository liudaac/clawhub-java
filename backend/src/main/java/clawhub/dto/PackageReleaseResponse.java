package clawhub.dto;

import clawhub.entity.PackageRelease;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageReleaseResponse {

    private UUID id;
    private UUID packageId;
    private String packageName;
    private String version;
    private String changelog;
    private String changelogSource;
    private List<FileInfo> files;
    private String integritySha256;
    private Map<String, Object> compatibility;
    private Map<String, Object> capabilities;
    private Map<String, Object> verification;
    private VirusTotalAnalysis vtAnalysis;
    private LlmSecurityAnalysis llmAnalysis;
    private StaticScanResult staticScan;
    private UUID publishedBy;
    private String publishedByHandle;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String path;
        private Long size;
        private String sha256;
        private String contentType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VirusTotalAnalysis {
        private String scanId;
        private String status;
        private Integer maliciousCount;
        private Integer suspiciousCount;
        private Integer harmlessCount;
        private Integer undetectedCount;
        private String permalink;
        private Instant scannedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LlmSecurityAnalysis {
        private String verdict;
        private String confidence;
        private String summary;
        private String guidance;
        private Instant analyzedAt;
        private String model;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaticScanResult {
        private String status;
        private Integer findingCount;
        private Instant scannedAt;
    }

    public static PackageReleaseResponse fromEntity(PackageRelease release) {
        if (release == null) {
            return null;
        }

        PackageReleaseResponse response = PackageReleaseResponse.builder()
                .id(release.getId())
                .packageId(release.getPackage_() != null ? release.getPackage_().getId() : null)
                .packageName(release.getPackage_() != null ? release.getPackage_().getName() : null)
                .version(release.getVersion())
                .changelog(release.getChangelog())
                .changelogSource(release.getChangelogSource() != null ? 
                        release.getChangelogSource().name().toLowerCase() : null)
                .integritySha256(release.getIntegritySha256())
                .compatibility(release.getCompatibility())
                .capabilities(release.getCapabilities())
                .verification(release.getVerification())
                .publishedBy(release.getPublishedBy())
                .createdAt(release.getCreatedAt())
                .build();

        // Convert files
        if (release.getFiles() != null) {
            response.setFiles(release.getFiles().stream()
                    .map(f -> FileInfo.builder()
                            .path(f.getPath())
                            .size(f.getSize())
                            .sha256(f.getSha256())
                            .contentType(f.getContentType())
                            .build())
                    .toList());
        }

        // Convert VT analysis
        if (release.getVtAnalysis() != null) {
            response.setVtAnalysis(VirusTotalAnalysis.builder()
                    .scanId(release.getVtAnalysis().getScanId())
                    .status(release.getVtAnalysis().getStatus())
                    .maliciousCount(release.getVtAnalysis().getMaliciousCount())
                    .suspiciousCount(release.getVtAnalysis().getSuspiciousCount())
                    .harmlessCount(release.getVtAnalysis().getHarmlessCount())
                    .undetectedCount(release.getVtAnalysis().getUndetectedCount())
                    .permalink(release.getVtAnalysis().getPermalink())
                    .scannedAt(release.getVtAnalysis().getScannedAt())
                    .build());
        }

        // Convert LLM analysis
        if (release.getLlmAnalysis() != null) {
            response.setLlmAnalysis(LlmSecurityAnalysis.builder()
                    .verdict(release.getLlmAnalysis().getVerdict())
                    .confidence(release.getLlmAnalysis().getConfidence())
                    .summary(release.getLlmAnalysis().getSummary())
                    .guidance(release.getLlmAnalysis().getGuidance())
                    .analyzedAt(release.getLlmAnalysis().getAnalyzedAt())
                    .model(release.getLlmAnalysis().getModel())
                    .build());
        }

        // Convert static scan
        if (release.getStaticScan() != null) {
            int findingCount = release.getStaticScan().getFindings() != null ? 
                    release.getStaticScan().getFindings().size() : 0;
            response.setStaticScan(StaticScanResult.builder()
                    .status(release.getStaticScan().getStatus())
                    .findingCount(findingCount)
                    .scannedAt(release.getStaticScan().getScannedAt())
                    .build());
        }

        return response;
    }
}
