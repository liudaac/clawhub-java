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
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_version_id")
    private SkillVersion latestVersion;

    @Column(name = "resource_id")
    private String resourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canonical_skill_id")
    private Skill canonicalSkill;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> forkOf;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> badges = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.ACTIVE;

    @Type(JsonType.class)
    @Column(name = "moderation_flags", columnDefinition = "jsonb")
    @Builder.Default
    private java.util.List<String> moderationFlags = java.util.List.of();

    @Column(name = "moderation_verdict")
    private String moderationVerdict;

    @Column(name = "moderation_notes", columnDefinition = "TEXT")
    private String moderationNotes;

    @Column(name = "moderation_reason", columnDefinition = "TEXT")
    private String moderationReason;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hidden_by")
    private User hiddenBy;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "report_count")
    @Builder.Default
    private Integer reportCount = 0;

    @Column(name = "stats_downloads")
    @Builder.Default
    private Long statsDownloads = 0L;

    @Column(name = "stats_stars")
    @Builder.Default
    private Integer statsStars = 0;

    @Column(name = "stats_versions")
    @Builder.Default
    private Integer statsVersions = 0;

    @Column(name = "stats_comments")
    @Builder.Default
    private Integer statsComments = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum ModerationStatus {
        ACTIVE, HIDDEN, REMOVED
    }

    public boolean isActive() {
        return moderationStatus == ModerationStatus.ACTIVE;
    }

    public boolean isHidden() {
        return moderationStatus == ModerationStatus.HIDDEN;
    }

    public boolean isRemoved() {
        return moderationStatus == ModerationStatus.REMOVED;
    }
}
