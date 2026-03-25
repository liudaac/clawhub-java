package clawhub.service;

import clawhub.dto.*;
import clawhub.entity.Package;
import clawhub.entity.PackageRelease;
import clawhub.entity.Publisher;
import clawhub.entity.User;
import clawhub.exception.BadRequestException;
import clawhub.exception.ForbiddenException;
import clawhub.exception.ResourceNotFoundException;
import clawhub.repository.PackageReleaseRepository;
import clawhub.repository.PackageRepository;
import clawhub.repository.PublisherMemberRepository;
import clawhub.repository.PublisherRepository;
import clawhub.repository.UserRepository;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {

    private final PackageRepository packageRepository;
    private final PackageReleaseRepository packageReleaseRepository;
    private final UserRepository userRepository;
    private final PublisherRepository publisherRepository;
    private final PublisherMemberRepository publisherMemberRepository;

    @Transactional
    public PackageResponse createPackage(PackageCreateRequest request, UUID currentUserId) {
        // Normalize name
        String normalizedName = request.getName().toLowerCase();

        // Check if package name already exists
        if (packageRepository.existsByNormalizedNameAndSoftDeletedAtIsNull(normalizedName)) {
            throw new BadRequestException("Package '" + request.getName() + "' already exists");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate publisher ownership
        UUID ownerPublisherId = request.getOwnerPublisherId();
        if (ownerPublisherId != null) {
            Publisher publisher = publisherRepository.findByIdAndDeletedAtIsNull(ownerPublisherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));
            // Check if user can publish to this publisher
            if (!publisherMemberRepository.canPublish(ownerPublisherId, currentUserId)) {
                throw new ForbiddenException("You don't have permission to publish to this publisher");
            }
        }

        // Create package
        Package pkg = Package.builder()
                .name(request.getName())
                .normalizedName(normalizedName)
                .displayName(request.getDisplayName())
                .summary(request.getSummary())
                .ownerUser(currentUser)
                .ownerPublisherId(ownerPublisherId)
                .family(mapFamily(request.getFamily()))
                .channel(request.getChannel() != null ? mapChannel(request.getChannel()) : Package.Channel.COMMUNITY)
                .isOfficial(false)
                .runtimeId(request.getRuntimeId())
                .compatibility(request.getCompatibility())
                .capabilities(request.getCapabilities())
                .scanStatus(Package.ScanStatus.NOT_RUN)
                .statsDownloads(0L)
                .statsInstalls(0L)
                .statsStars(0)
                .statsVersions(0)
                .build();

        Package saved = packageRepository.save(pkg);
        log.info("Created package: {} (id: {}) by user: {}", saved.getName(), saved.getId(), currentUserId);

        return PackageResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PackageResponse getPackage(String name) {
        String normalizedName = name.toLowerCase();
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + name));

        // Get latest version
        Optional<PackageRelease> latestRelease = packageReleaseRepository.findLatestByPackageId(pkg.getId());
        String latestVersion = latestRelease.map(PackageRelease::getVersion).orElse(null);

        return PackageResponse.fromEntityWithLatestVersion(pkg, latestVersion);
    }

    @Transactional(readOnly = true)
    public PackageResponse getPackageById(UUID id) {
        Package pkg = packageRepository.findByIdAndSoftDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + id));

        Optional<PackageRelease> latestRelease = packageReleaseRepository.findLatestByPackageId(pkg.getId());
        String latestVersion = latestRelease.map(PackageRelease::getVersion).orElse(null);

        return PackageResponse.fromEntityWithLatestVersion(pkg, latestVersion);
    }

    @Transactional(readOnly = true)
    public PackageListResponse listPackages(String search, String family, String channel, 
                                            Boolean official, String runtimeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "isOfficial", "statsDownloads"));

        Package.Family familyFilter = null;
        if (family != null && !family.isEmpty()) {
            try {
                familyFilter = Package.Family.valueOf(family.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid filter, ignore
            }
        }

        Package.Channel channelFilter = null;
        if (channel != null && !channel.isEmpty()) {
            try {
                channelFilter = Package.Channel.valueOf(channel.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid filter, ignore
            }
        }

        Page<Package> packages = packageRepository.searchPackages(
                search, familyFilter, channelFilter, official, runtimeId, pageable);

        List<PackageResponse> responses = packages.getContent().stream()
                .map(pkg -> {
                    Optional<PackageRelease> latest = packageReleaseRepository.findLatestByPackageId(pkg.getId());
                    return PackageResponse.fromEntityWithLatestVersion(pkg, 
                            latest.map(PackageRelease::getVersion).orElse(null));
                })
                .toList();

        return PackageListResponse.builder()
                .packages(responses)
                .total(packages.getTotalElements())
                .page(page)
                .size(size)
                .hasMore(packages.hasNext())
                .build();
    }

    @Transactional
    public PackageResponse updatePackage(String name, PackageUpdateRequest request, UUID currentUserId) {
        String normalizedName = name.toLowerCase();
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + name));

        // Check permission
        if (!canModifyPackage(pkg, currentUserId)) {
            throw new ForbiddenException("You don't have permission to update this package");
        }

        if (request.getDisplayName() != null) {
            pkg.setDisplayName(request.getDisplayName());
        }
        if (request.getSummary() != null) {
            pkg.setSummary(request.getSummary());
        }
        if (request.getCompatibility() != null) {
            pkg.setCompatibility(request.getCompatibility());
        }
        if (request.getCapabilities() != null) {
            pkg.setCapabilities(request.getCompatibility());
        }

        Package updated = packageRepository.save(pkg);
        log.info("Updated package: {} by user: {}", name, currentUserId);

        return PackageResponse.fromEntity(updated);
    }

    @Transactional
    public void deletePackage(String name, UUID currentUserId) {
        String normalizedName = name.toLowerCase();
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + name));

        // Only owner or admin can delete
        if (!canModifyPackage(pkg, currentUserId)) {
            throw new ForbiddenException("You don't have permission to delete this package");
        }

        pkg.setSoftDeletedAt(Instant.now());
        packageRepository.save(pkg);
        log.info("Deleted package: {} by user: {}", name, currentUserId);
    }

    @Transactional
    public void incrementDownloads(String name) {
        String normalizedName = name.toLowerCase();
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(normalizedName)
                .orElse(null);
        if (pkg != null) {
            pkg.setStatsDownloads(pkg.getStatsDownloads() + 1);
            packageRepository.save(pkg);
        }
    }

    @Transactional
    public void incrementInstalls(String name) {
        String normalizedName = name.toLowerCase();
        Package pkg = packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(normalizedName)
                .orElse(null);
        if (pkg != null) {
            pkg.setStatsInstalls(pkg.getStatsInstalls() + 1);
            packageRepository.save(pkg);
        }
    }

    @Transactional(readOnly = true)
    public Package getPackageEntity(String name) {
        String normalizedName = name.toLowerCase();
        return packageRepository.findByNormalizedNameAndSoftDeletedAtIsNull(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + name));
    }

    @Transactional(readOnly = true)
    public Package getPackageEntityById(UUID id) {
        return packageRepository.findByIdAndSoftDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + id));
    }

    private boolean canModifyPackage(Package pkg, UUID userId) {
        // Owner can always modify
        if (pkg.getOwnerUser().getId().equals(userId)) {
            return true;
        }
        // Publisher members can modify
        if (pkg.getOwnerPublisherId() != null) {
            return publisherMemberRepository.canPublish(pkg.getOwnerPublisherId(), userId);
        }
        return false;
    }

    private Package.Family mapFamily(PackageCreateRequest.Family family) {
        return switch (family) {
            case SKILL -> Package.Family.SKILL;
            case CODE_PLUGIN -> Package.Family.CODE_PLUGIN;
            case BUNDLE_PLUGIN -> Package.Family.BUNDLE_PLUGIN;
        };
    }

    private Package.Channel mapChannel(PackageCreateRequest.Channel channel) {
        return switch (channel) {
            case OFFICIAL -> Package.Channel.OFFICIAL;
            case COMMUNITY -> Package.Channel.COMMUNITY;
            case PRIVATE -> Package.Channel.PRIVATE;
        };
    }
}