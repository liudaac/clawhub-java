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
@Table(name = "skill_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String changelog;

    @Column(name = "changelog_source")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChangelogSource changelogSource = ChangelogSource.AUTO;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "sha256_hash")
    private String sha256Hash;

    @Type(JsonType.class)
    @Column(name = "files", columnDefinition = "jsonb")
    private List<FileInfo> files;

    @Type(JsonType.class)
    @Column(name = "parsed_metadata", columnDefinition = "jsonb")
    private Map<String, Object> parsedMetadata;

    // VirusTotal Analysis
    @Type(JsonType.class)
    @Column(name = "vt_analysis", columnDefinition = "jsonb")
    private VirusTotalAnalysis vtAnalysis;

    // LLM Security Analysis
    @Type(JsonType.class)
    @Column(name = "llm_analysis", columnDefinition = "jsonb")
    private LlmSecurityAnalysis llmAnalysis;

    // Moderation Snapshot
    @Type(JsonType.class)
    @Column(name = "moderation_snapshot", columnDefinition = "jsonb")
    private ModerationSnapshot moderationSnapshot;

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
        private String status; // pending, completed, error
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
            private String category; // malicious, suspicious, harmless, undetected
            private String result;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LlmSecurityAnalysis {
        private String verdict; // benign, suspicious, malicious
        private String confidence; // high, medium, low
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
            private String severity; // info, warn, critical
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
    public static class ModerationSnapshot {
        private String verdict; // clean, suspicious, malicious
        private List<String> reasonCodes;
        private List<ModerationFinding> evidence;
        private String summary;
        private String engineVersion;
        private Instant evaluatedAt;
        private UUID sourceVersionId;
        private List<String> legacyFlags;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ModerationFinding {
            private String code;
            private String severity;
            private String file;
            private Integer line;
            private String message;
            private String evidence;
        }
    }
}
