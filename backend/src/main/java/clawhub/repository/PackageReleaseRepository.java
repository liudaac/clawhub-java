package clawhub.repository;

import clawhub.entity.PackageRelease;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PackageReleaseRepository extends JpaRepository<PackageRelease, UUID> {

    List<PackageRelease> findByPackage_Id(UUID packageId);

    List<PackageRelease> findByPackage_IdAndSoftDeletedAtIsNullOrderByCreatedAtDesc(UUID packageId);

    Optional<PackageRelease> findByPackage_IdAndVersion(UUID packageId, String version);

    Optional<PackageRelease> findByPackage_IdAndVersionAndSoftDeletedAtIsNull(UUID packageId, String version);

    Optional<PackageRelease> findByIdAndSoftDeletedAtIsNull(UUID id);

    @Query("SELECT pr FROM PackageRelease pr WHERE pr.package_.id = :packageId AND pr.softDeletedAt IS NULL " +
           "ORDER BY pr.createdAt DESC")
    Page<PackageRelease> findByPackageId(@Param("packageId") UUID packageId, Pageable pageable);

    @Query("SELECT pr FROM PackageRelease pr WHERE pr.package_.normalizedName = :packageName AND pr.softDeletedAt IS NULL " +
           "ORDER BY pr.createdAt DESC")
    List<PackageRelease> findByPackageName(@Param("packageName") String packageName);

    @Query("SELECT COUNT(pr) FROM PackageRelease pr WHERE pr.package_.id = :packageId AND pr.softDeletedAt IS NULL")
    long countByPackageId(@Param("packageId") UUID packageId);

    @Query("SELECT pr FROM PackageRelease pr WHERE pr.package_.id = :packageId AND pr.softDeletedAt IS NULL " +
           "ORDER BY pr.createdAt DESC LIMIT 1")
    Optional<PackageRelease> findLatestByPackageId(@Param("packageId") UUID packageId);

    boolean existsByPackage_IdAndVersion(UUID packageId, String version);

    boolean existsByPackage_IdAndVersionAndSoftDeletedAtIsNull(UUID packageId, String version);

    @Query("SELECT pr FROM PackageRelease pr WHERE pr.publishedBy = :userId AND pr.softDeletedAt IS NULL " +
           "ORDER BY pr.createdAt DESC")
    Page<PackageRelease> findByPublishedBy(@Param("userId") UUID userId, Pageable pageable);
}
