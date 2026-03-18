package clawhub.repository;

import clawhub.entity.Skill;
import clawhub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {

    Optional<Skill> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Skill> findByOwner(User owner, Pageable pageable);

    Page<Skill> findByModerationStatus(Skill.ModerationStatus status, Pageable pageable);

    Page<Skill> findByModerationStatusAndOwner(Skill.ModerationStatus status, User owner, Pageable pageable);

    @Query("SELECT s FROM Skill s WHERE s.moderationStatus = 'ACTIVE' " +
           "AND (:nonSuspiciousOnly = false OR s.moderationVerdict IS NULL OR s.moderationVerdict = 'clean') " +
           "ORDER BY s.statsDownloads DESC")
    Page<Skill> findPublicByDownloads(@Param("nonSuspiciousOnly") boolean nonSuspiciousOnly, Pageable pageable);

    @Query("SELECT s FROM Skill s WHERE s.moderationStatus = 'ACTIVE' " +
           "AND (:nonSuspiciousOnly = false OR s.moderationVerdict IS NULL OR s.moderationVerdict = 'clean') " +
           "ORDER BY s.statsStars DESC")
    Page<Skill> findPublicByStars(@Param("nonSuspiciousOnly") boolean nonSuspiciousOnly, Pageable pageable);

    @Query("SELECT s FROM Skill s WHERE s.moderationStatus = 'ACTIVE' " +
           "AND (:nonSuspiciousOnly = false OR s.moderationVerdict IS NULL OR s.moderationVerdict = 'clean') " +
           "ORDER BY s.createdAt DESC")
    Page<Skill> findPublicByCreated(@Param("nonSuspiciousOnly") boolean nonSuspiciousOnly, Pageable pageable);

    @Query("SELECT s FROM Skill s WHERE s.moderationStatus = 'ACTIVE' " +
           "AND s.badges IS NOT NULL AND jsonb_exists(s.badges, 'highlighted') = true")
    Page<Skill> findHighlighted(Pageable pageable);

    @Modifying
    @Query("UPDATE Skill s SET s.statsDownloads = s.statsDownloads + 1 WHERE s.id = :skillId")
    void incrementDownloads(@Param("skillId") UUID skillId);

    @Modifying
    @Query("UPDATE Skill s SET s.statsVersions = s.statsVersions + 1 WHERE s.id = :skillId")
    void incrementVersions(@Param("skillId") UUID skillId);

    @Modifying
    @Query("UPDATE Skill s SET s.statsComments = s.statsComments + 1 WHERE s.id = :skillId")
    void incrementComments(@Param("skillId") UUID skillId);

    @Modifying
    @Query("UPDATE Skill s SET s.statsComments = s.statsComments - 1 WHERE s.id = :skillId AND s.statsComments > 0")
    void decrementComments(@Param("skillId") UUID skillId);

    @Modifying
    @Query("UPDATE Skill s SET s.statsStars = s.statsStars + 1 WHERE s.id = :skillId")
    void incrementStars(@Param("skillId") UUID skillId);

    @Modifying
    @Query("UPDATE Skill s SET s.statsStars = s.statsStars - 1 WHERE s.id = :skillId AND s.statsStars > 0")
    void decrementStars(@Param("skillId") UUID skillId);

    Page<Skill> findByModerationStatus(Skill.ModerationStatus status, Pageable pageable);

    Page<Skill> findByModerationStatusAndReportCountGreaterThan(
            Skill.ModerationStatus status, int reportCount, Pageable pageable);
}
