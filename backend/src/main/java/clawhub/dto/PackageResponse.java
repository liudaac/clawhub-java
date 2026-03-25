package clawhub.dto;

import clawhub.entity.Package;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageResponse {

    private UUID id;
    private String name;
    private String displayName;
    private String family;
    private String runtimeId;
    private String channel;
    private Boolean isOfficial;
    private String summary;
    private UUID ownerUserId;
    private String ownerHandle;
    private UUID ownerPublisherId;
    private String ownerPublisherHandle;
    private Map<String, Object> compatibility;
    private Map<String, Object> capabilities;
    private Map<String, Object> verification;
    private String scanStatus;
    private Long statsDownloads;
    private Long statsInstalls;
    private Integer statsStars;
    private Integer statsVersions;
    private String latestVersion;
    private Instant createdAt;
    private Instant updatedAt;

    // Computed fields
    private Boolean executesCode;
    private String verificationTier;

    public static PackageResponse fromEntity(Package pkg) {
        if (pkg == null) {
            return null;
        }
        return PackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .displayName(pkg.getDisplayName())
                .family(pkg.getFamily() != null ? pkg.getFamily().name().toLowerCase() : null)
                .runtimeId(pkg.getRuntimeId())
                .channel(pkg.getChannel() != null ? pkg.getChannel().name().toLowerCase() : null)
                .isOfficial(pkg.getIsOfficial())
                .summary(pkg.getSummary())
                .ownerUserId(pkg.getOwnerUser() != null ? pkg.getOwnerUser().getId() : null)
                .ownerHandle(pkg.getOwnerUser() != null ? pkg.getOwnerUser().getHandle() : null)
                .ownerPublisherId(pkg.getOwnerPublisherId())
                .compatibility(pkg.getCompatibility())
                .capabilities(pkg.getCapabilities())
                .verification(pkg.getVerification())
                .scanStatus(pkg.getScanStatus() != null ? pkg.getScanStatus().name().toLowerCase() : null)
                .statsDownloads(pkg.getStatsDownloads())
                .statsInstalls(pkg.getStatsInstalls())
                .statsStars(pkg.getStatsStars())
                .statsVersions(pkg.getStatsVersions())
                .createdAt(pkg.getCreatedAt())
                .updatedAt(pkg.getUpdatedAt())
                .executesCode(pkg.executesCode())
                .verificationTier(pkg.getVerificationTier())
                .build();
    }

    public static PackageResponse fromEntityWithLatestVersion(Package pkg, String latestVersion) {
        PackageResponse response = fromEntity(pkg);
        if (response != null) {
            response.setLatestVersion(latestVersion);
        }
        return response;
    }
}
