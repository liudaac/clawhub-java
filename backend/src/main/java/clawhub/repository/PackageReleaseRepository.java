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

    // ==================== Scan Backfill Methods ====================

    /**
     * 查找扫描状态为 PENDING 的发布版本（按创建时间升序）
     * 对应原版: getPackageReleaseScanBackfillBatchInternal
     */
    @Query("SELECT pr FROM PackageRelease pr WHERE pr.package_.scanStatus = 'PENDING' " +
           "AND pr.softDeletedAt IS NULL ORDER BY pr.createdAt ASC")
    List<PackageRelease> findByScanStatusPendingOrderByCreatedAtAsc(Pageable pageable);

    /**
     * 查找最近的 N 个发布版本（按创建时间降序）
     * 用于优先扫描最近发布的包
     */
    @Query("SELECT pr FROM PackageRelease pr WHERE pr.softDeletedAt IS NULL " +
           "ORDER BY pr.createdAt DESC")
    List<PackageRelease> findTopNByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 查找需要回扫的发布版本批次
     * 优先扫描最近发布的，再处理积压的
     */
    @Query("SELECT pr FROM PackageRelease pr WHERE pr.package_.scanStatus IN ('NOT_RUN', 'PENDING') " +
           "AND pr.softDeletedAt IS NULL ORDER BY pr.createdAt DESC")
    List<PackageRelease> findBackfillBatchOrderByCreatedAtDesc(Pageable pageable);
}
