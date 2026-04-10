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
@Table(name = "skill_trusted_publishers", indexes = {
    @Index(name = "idx_trusted_publisher_skill", columnList = "skill_id"),
    @Index(name = "idx_trusted_publisher_repo", columnList = "repository")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillTrustedPublisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

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
    @Builder.Default
    private String environment = "production";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
