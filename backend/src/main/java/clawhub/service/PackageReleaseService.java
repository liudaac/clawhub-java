package clawhub.service;

import clawhub.dto.PackageReleaseRequest;
import clawhub.dto.PackageReleaseResponse;
import clawhub.dto.PackageReleaseListResponse;
import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import clawhub.exception.BadRequestException;
import clawhub.exception.ForbiddenException;
import clawhub.exception.ResourceNotFoundException;
import clawhub.repository.PackageReleaseRepository;
import clawhub.repository.PackageRepository;
import clawhub.repository.PublisherMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageReleaseService {

    private final PackageReleaseRepository packageReleaseRepository;
    private final PackageRepository packageRepository;
    private final PublisherMemberRepository publisherMemberRepository;

    @Transactional
    public PackageReleaseResponse createRelease(String packageName, PackageReleaseRequest request, UUID currentUserId) {
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(packageName.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageName));

        // Check permission
        if (!canPublishToPackage(pkg, currentUserId)) {
            throw new ForbiddenException("You don't have permission to publish to this package");
        }

        // Check if version already exists
        if (packageReleaseRepository.existsByPackage_IdAndVersionAndSoftDeletedAtIsNull(pkg.getId(), request.getVersion())) {
            throw new BadRequestException("Version '" + request.getVersion() + "' already exists for this package");
        }

        // Convert files
        List<PackageRelease.FileInfo> files = request.getFiles().stream()
                .map(f -> PackageRelease.FileInfo.builder()
                        .path(f.getPath())
                        .size(f.getSize())
                        .sha256(f.getSha256())
                        .contentType(f.getContentType())
                        .build())
                .toList();

        // Create release
        PackageRelease release = PackageRelease.builder()
                .package_(pkg)
                .version(request.getVersion())
                .changelog(request.getChangelog())
                .changelogSource(PackageRelease.ChangelogSource.USER)
                .files(files)
                .integritySha256(request.getIntegritySha256())
                .compatibility(request.getCompatibility())
                .capabilities(request.getCapabilities())
                .publishedBy(currentUserId)
                .build();

        PackageRelease saved = packageReleaseRepository.save(release);

        // Update package stats
        pkg.setStatsVersions(pkg.getStatsVersions() + 1);
        packageRepository.save(pkg);

        log.info("Created release: {} v{} for package: {}", 
                request.getVersion(), packageName, currentUserId);

        return PackageReleaseResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PackageReleaseResponse getRelease(String packageName, String version) {
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(packageName.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageName));

        PackageRelease release = packageReleaseRepository
                .findByPackage_IdAndVersionAndSoftDeletedAtIsNull(pkg.getId(), version)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found: " + version));

        return PackageReleaseResponse.fromEntity(release);
    }

    @Transactional(readOnly = true)
    public PackageReleaseListResponse listReleases(String packageName, int page, int size) {
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(packageName.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageName));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PackageRelease> releases = packageReleaseRepository.findByPackageId(pkg.getId(), pageable);

        List<PackageReleaseResponse> responses = releases.getContent().stream()
                .map(PackageReleaseResponse::fromEntity)
                .toList();

        return PackageReleaseListResponse.builder()
                .releases(responses)
                .total(releases.getTotalElements())
                .page(page)
                .size(size)
                .hasMore(releases.hasNext())
                .build();
    }

    @Transactional
    public void deleteRelease(String packageName, String version, UUID currentUserId) {
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(packageName.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageName));

        PackageRelease release = packageReleaseRepository
                .findByPackage_IdAndVersionAndSoftDeletedAtIsNull(pkg.getId(), version)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found: " + version));

        // Check permission
        if (!canPublishToPackage(pkg, currentUserId)) {
            throw new ForbiddenException("You don't have permission to delete this release");
        }

        release.setSoftDeletedAt(Instant.now());
        packageReleaseRepository.save(release);

        // Update package stats
        long remainingVersions = packageReleaseRepository.countByPackageId(pkg.getId());
        pkg.setStatsVersions((int) remainingVersions);
        packageRepository.save(pkg);

        log.info("Deleted release: {} v{} by user: {}", packageName, version, currentUserId);
    }

    @Transactional(readOnly = true)
    public PackageReleaseResponse getLatestRelease(String packageName) {
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(packageName.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageName));

        PackageRelease release = packageReleaseRepository.findLatestByPackageId(pkg.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No releases found for package: " + packageName));

        return PackageReleaseResponse.fromEntity(release);
    }

    /**
     * 获取发布版本实体（用于下载安全检查）
     */
    @Transactional(readOnly = true)
    public PackageRelease findReleaseEntity(String packageName, String version) {
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(packageName.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageName));

        return packageReleaseRepository
                .findByPackage_IdAndVersionAndSoftDeletedAtIsNull(pkg.getId(), version)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found: " + version));
    }

    private boolean canPublishToPackage(Package pkg, UUID userId) {
        // Owner can always publish
        if (pkg.getOwnerUser().getId().equals(userId)) {
            return true;
        }
        // Publisher members can publish
        if (pkg.getOwnerPublisherId() != null) {
            return publisherMemberRepository.canPublish(pkg.getOwnerPublisherId(), userId);
        }
        return false;
    }
}
