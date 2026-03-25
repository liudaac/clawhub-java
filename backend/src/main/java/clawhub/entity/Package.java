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
@Table(name = "packages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true)
    private String normalizedName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(name = "owner_publisher_id")
    private UUID ownerPublisherId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Family family;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Channel channel = Channel.COMMUNITY;

    @Column(name = "is_official")
    @Builder.Default
    private Boolean isOfficial = false;

    @Column(name = "runtime_id")
    private String runtimeId;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> compatibility;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> capabilities;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> verification;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status")
    @Builder.Default
    private ScanStatus scanStatus = ScanStatus.NOT_RUN;

    @Column(name = "stats_downloads")
    @Builder.Default
    private Long statsDownloads = 0L;

    @Column(name = "stats_installs")
    @Builder.Default
    private Long statsInstalls = 0L;

    @Column(name = "stats_stars")
    @Builder.Default
    private Integer statsStars = 0;

    @Column(name = "stats_versions")
    @Builder.Default
    private Integer statsVersions = 0;

    @Column(name = "soft_deleted_at")
    private Instant softDeletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Family {
        SKILL, CODE_PLUGIN, BUNDLE_PLUGIN
    }

    public enum Channel {
        OFFICIAL, COMMUNITY, PRIVATE
    }

    public enum ScanStatus {
        NOT_RUN, PENDING, CLEAN, SUSPICIOUS, MALICIOUS
    }

    public boolean isSkill() {
        return family == Family.SKILL;
    }

    public boolean isCodePlugin() {
        return family == Family.CODE_PLUGIN;
    }

    public boolean isBundlePlugin() {
        return family == Family.BUNDLE_PLUGIN;
    }

    public boolean isOfficial() {
        return channel == Channel.OFFICIAL || Boolean.TRUE.equals(isOfficial);
    }

    public boolean isCommunity() {
        return channel == Channel.COMMUNITY;
    }

    public boolean isPrivate() {
        return channel == Channel.PRIVATE;
    }

    public boolean isDeleted() {
        return softDeletedAt != null;
    }

    public boolean executesCode() {
        if (capabilities == null) {
            return false;
        }
        Object executesCode = capabilities.get("executesCode");
        return Boolean.TRUE.equals(executesCode);
    }

    public String getVerificationTier() {
        if (verification == null) {
            return "none";
        }
        Object tier = verification.get("tier");
        return tier != null ? tier.toString() : "none";
    }
}
