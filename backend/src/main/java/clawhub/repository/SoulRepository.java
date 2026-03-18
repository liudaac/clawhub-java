package clawhub.repository;

import clawhub.entity.Soul;
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
public interface SoulRepository extends JpaRepository<Soul, UUID> {

    Optional<Soul> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Soul> findByOwner(User owner, Pageable pageable);

    Page<Soul> findByStatus(Soul.Status status, Pageable pageable);

    @Query("SELECT s FROM Soul s WHERE s.status = 'ACTIVE' ORDER BY s.statsDownloads DESC")
    Page<Soul> findPublicByDownloads(Pageable pageable);

    @Query("SELECT s FROM Soul s WHERE s.status = 'ACTIVE' ORDER BY s.statsStars DESC")
    Page<Soul> findPublicByStars(Pageable pageable);

    @Query("SELECT s FROM Soul s WHERE s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    Page<Soul> findPublicByCreated(Pageable pageable);

    @Modifying
    @Query("UPDATE Soul s SET s.statsDownloads = s.statsDownloads + 1 WHERE s.id = :soulId")
    void incrementDownloads(@Param("soulId") UUID soulId);

    @Modifying
    @Query("UPDATE Soul s SET s.statsVersions = s.statsVersions + 1 WHERE s.id = :soulId")
    void incrementVersions(@Param("soulId") UUID soulId);

    @Modifying
    @Query("UPDATE Soul s SET s.statsComments = s.statsComments + 1 WHERE s.id = :soulId")
    void incrementComments(@Param("soulId") UUID soulId);

    @Modifying
    @Query("UPDATE Soul s SET s.statsComments = s.statsComments - 1 WHERE s.id = :soulId AND s.statsComments > 0")
    void decrementComments(@Param("soulId") UUID soulId);

    @Modifying
    @Query("UPDATE Soul s SET s.statsStars = s.statsStars + 1 WHERE s.id = :soulId")
    void incrementStars(@Param("soulId") UUID soulId);

    @Modifying
    @Query("UPDATE Soul s SET s.statsStars = s.statsStars - 1 WHERE s.id = :soulId AND s.statsStars > 0")
    void decrementStars(@Param("soulId") UUID soulId);
}
