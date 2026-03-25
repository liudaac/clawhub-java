package clawhub.repository;

import clawhub.entity.Package;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PackageRepository extends JpaRepository<Package, UUID> {

    Optional<Package> findByNormalizedName(String normalizedName);

    Optional<Package> findByNormalizedNameAndSoftDeletedAtIsNull(String normalizedName);

    Optional<Package> findByIdAndSoftDeletedAtIsNull(UUID id);

    boolean existsByNormalizedName(String normalizedName);

    boolean existsByNormalizedNameAndSoftDeletedAtIsNull(String normalizedName);

    @Query("SELECT p FROM Package p WHERE p.softDeletedAt IS NULL AND " +
           "(:family IS NULL OR p.family = :family) AND " +
           "(:channel IS NULL OR p.channel = :channel) AND " +
           "(:official IS NULL OR p.isOfficial = :official) AND " +
           "(:runtimeId IS NULL OR p.runtimeId = :runtimeId) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.summary) LIKE LOWER(CONCAT('%', :search, '%')))" +
           "ORDER BY p.isOfficial DESC, p.statsDownloads DESC")
    Page<Package> searchPackages(
            @Param("search") String search,
            @Param("family") Package.Family family,
            @Param("channel") Package.Channel channel,
            @Param("official") Boolean official,
            @Param("runtimeId") String runtimeId,
            Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.softDeletedAt IS NULL " +
           "ORDER BY p.isOfficial DESC, p.statsDownloads DESC")
    Page<Package> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.softDeletedAt IS NULL AND p.family = :family " +
           "ORDER BY p.isOfficial DESC, p.statsDownloads DESC")
    Page<Package> findByFamily(@Param("family") Package.Family family, Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.softDeletedAt IS NULL AND p.ownerUser.id = :userId " +
           "ORDER BY p.updatedAt DESC")
    Page<Package> findByOwnerUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.softDeletedAt IS NULL AND p.ownerPublisherId = :publisherId " +
           "ORDER BY p.updatedAt DESC")
    Page<Package> findByOwnerPublisherId(@Param("publisherId") UUID publisherId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Package p WHERE p.softDeletedAt IS NULL AND p.ownerPublisherId = :publisherId")
    long countByOwnerPublisherId(@Param("publisherId") UUID publisherId);

    @Query("SELECT p FROM Package p WHERE p.softDeletedAt IS NULL AND " +
           "EXISTS (SELECT 1 FROM p.capabilities c WHERE KEY(c) = :capability)")
    Page<Package> findByCapability(@Param("capability") String capability, Pageable pageable);
}
