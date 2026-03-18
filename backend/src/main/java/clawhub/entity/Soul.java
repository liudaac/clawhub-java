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
@Table(name = "souls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Soul {

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
    private SoulVersion latestVersion;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> tags = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

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

    public enum Status {
        ACTIVE
    }
}
