package clawhub.repository;

import clawhub.entity.Publisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, UUID> {

    Optional<Publisher> findByHandle(String handle);

    Optional<Publisher> findByHandleAndDeletedAtIsNull(String handle);

    Optional<Publisher> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Publisher> findByLinkedUserIdAndKindAndDeletedAtIsNull(UUID userId, Publisher.Kind kind);

    boolean existsByHandle(String handle);

    boolean existsByHandleAndDeletedAtIsNull(String handle);

    @Query("SELECT p FROM Publisher p WHERE p.deletedAt IS NULL AND " +
           "(:kind IS NULL OR p.kind = :kind) AND " +
           "(:trusted IS NULL OR p.trustedPublisher = :trusted) AND " +
           "(:search IS NULL OR LOWER(p.handle) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.displayName) LIKE LOWER(CONCAT('%', :search, '%')))" +
           "ORDER BY p.trustedPublisher DESC, p.statsDownloads DESC NULLS LAST")
    Page<Publisher> searchPublishers(
            @Param("search") String search,
            @Param("kind") Publisher.Kind kind,
            @Param("trusted") Boolean trusted,
            Pageable pageable);

    @Query("SELECT p FROM Publisher p WHERE p.deletedAt IS NULL AND p.deactivatedAt IS NULL " +
           "ORDER BY p.trustedPublisher DESC, p.createdAt DESC")
    Page<Publisher> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Publisher p WHERE p.deletedAt IS NULL AND p.linkedUserId = :userId")
    Optional<Publisher> findPersonalPublisherByUserId(@Param("userId") UUID userId);
}
