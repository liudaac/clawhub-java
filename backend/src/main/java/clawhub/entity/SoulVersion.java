package clawhub.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "soul_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoulVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "soul_id", nullable = false)
    private Soul soul;

    @Column(nullable = false)
    private String version;

    private String tag;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String changelog;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<SoulFile> files;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Object parsed;

    @Column(name = "embedding_id")
    private String embeddingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "soft_deleted_at")
    private Instant softDeletedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoulFile {
        private String path;
        private Long size;
        private String storageId;
        private String sha256;
    }

    public boolean isSoftDeleted() {
        return softDeletedAt != null;
    }
}
