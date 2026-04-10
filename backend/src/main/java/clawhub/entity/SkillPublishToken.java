package clawhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "skill_publish_tokens", indexes = {
    @Index(name = "idx_publish_token_hash", columnList = "token_hash"),
    @Index(name = "idx_publish_token_expires", columnList = "expires_at"),
    @Index(name = "idx_publish_token_skill", columnList = "skill_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillPublishToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(length = 50)
    private String version;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "github-actions";

    @Column(nullable = false, length = 255)
    private String repository;

    @Column(name = "repository_id", nullable = false, length = 50)
    private String repositoryId;

    @Column(name = "repository_owner", nullable = false, length = 100)
    private String repositoryOwner;

    @Column(name = "repository_owner_id", nullable = false, length = 50)
    private String repositoryOwnerId;

    @Column(name = "workflow_filename", nullable = false, length = 255)
    private String workflowFilename;

    @Column(nullable = false, length = 100)
    private String environment;

    @Column(name = "run_id", nullable = false, length = 50)
    private String runId;

    @Column(name = "run_attempt", nullable = false, length = 10)
    private String runAttempt;

    @Column(nullable = false, length = 100)
    private String sha;

    @Column(nullable = false, length = 255)
    private String ref;

    @Column(name = "ref_type", length = 50)
    private String refType;

    @Column(length = 100)
    private String actor;

    @Column(name = "actor_id", length = 50)
    private String actorId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}
