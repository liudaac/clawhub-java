package clawhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "publishers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Column(nullable = false, unique = true)
    private String handle;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 1024)
    private String image;

    @Column(name = "linked_user_id")
    private UUID linkedUserId;

    @Column(name = "trusted_publisher")
    @Builder.Default
    private Boolean trustedPublisher = false;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Kind {
        USER, ORG
    }

    public boolean isUser() {
        return kind == Kind.USER;
    }

    public boolean isOrg() {
        return kind == Kind.ORG;
    }

    public boolean isActive() {
        return deactivatedAt == null && deletedAt == null;
    }

    public boolean isDeactivated() {
        return deactivatedAt != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isTrusted() {
        return Boolean.TRUE.equals(trustedPublisher);
    }
}
