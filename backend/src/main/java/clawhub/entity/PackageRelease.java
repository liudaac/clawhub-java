package clawhub.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "package_releases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private Package package_;

    @Column(nullable = false)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String changelog;

    @Enumerated(EnumType.STRING)
    @Column(name = "changelog_source")
    @Builder.Default
    private ChangelogSource changelogSource = ChangelogSource.AUTO;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<FileInfo> files;

    @Column(name = "integrity_sha256")
    private String integritySha256;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> compatibility;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> capabilities;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> verification;

    @Type(JsonType.class)
    @Column(name = "vt_analysis", columnDefinition = "jsonb")
    private VirusTotalAnalysis vtAnalysis;

    @Type(JsonType.class)
    @Column(name = "llm_analysis", columnDefinition = "jsonb")
    private LlmSecurityAnalysis llmAnalysis;

    @Type(JsonType.class)
    @Column(name = "static_scan", columnDefinition = "jsonb")
    private StaticScanResult staticScan;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "soft_deleted_at")
    private Instant softDeletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum ChangelogSource {
        AUTO, USER
    }

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
        private Map<String, EngineResult> results;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EngineResult {
            private String engine;
            private String category;
            private String result;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LlmSecurityAnalysis {
        private String verdict;
        private String confidence;
        private String summary;
        private List<Dimension> dimensions;
        private String guidance;
        private List<Finding> findings;
        private Instant analyzedAt;
        private String model;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Dimension {
            private String name;
            private String label;
            private String rating;
            private String detail;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Finding {
            private String code;
            private String severity;
            private String file;
            private Integer line;
            private String message;
            private String evidence;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaticScanResult {
        private String status;
        private List<Finding> findings;
        private Instant scannedAt;
        private String scannerVersion;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Finding {
            private String code;
            private String severity;
            private String file;
            private Integer line;
            private String message;
            private String evidence;
        }
    }

    public boolean isDeleted() {
        return softDeletedAt != null;
    }

    public boolean hasSecurityScan() {
        return vtAnalysis != null || llmAnalysis != null || staticScan != null;
    }

    public boolean isSecurityClean() {
        boolean vtClean = vtAnalysis == null || 
            (vtAnalysis.getMaliciousCount() != null && vtAnalysis.getMaliciousCount() == 0);
        boolean llmClean = llmAnalysis == null || 
            !"malicious".equalsIgnoreCase(llmAnalysis.getVerdict());
        return vtClean && llmClean;
    }
}
