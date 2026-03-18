package clawhub.dto;

import clawhub.entity.Soul;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SoulResponse {
    private UUID id;
    private String slug;
    private String displayName;
    private String summary;
    private UserResponse owner;
    private SoulVersionResponse latestVersion;
    private Map<String, Object> tags;
    private Long statsDownloads;
    private Integer statsStars;
    private Integer statsVersions;
    private Integer statsComments;
    private Instant createdAt;
    private Instant updatedAt;

    public static SoulResponse fromEntity(Soul soul) {
        if (soul == null) return null;
        return SoulResponse.builder()
                .id(soul.getId())
                .slug(soul.getSlug())
                .displayName(soul.getDisplayName())
                .summary(soul.getSummary())
                .owner(UserResponse.fromEntity(soul.getOwner()))
                .latestVersion(soul.getLatestVersion() != null ? 
                        SoulVersionResponse.fromEntity(soul.getLatestVersion()) : null)
                .tags(soul.getTags())
                .statsDownloads(soul.getStatsDownloads())
                .statsStars(soul.getStatsStars())
                .statsVersions(soul.getStatsVersions())
                .statsComments(soul.getStatsComments())
                .createdAt(soul.getCreatedAt())
                .updatedAt(soul.getUpdatedAt())
                .build();
    }

    public static SoulResponse fromEntityMinimal(Soul soul) {
        if (soul == null) return null;
        return SoulResponse.builder()
                .id(soul.getId())
                .slug(soul.getSlug())
                .displayName(soul.getDisplayName())
                .summary(soul.getSummary())
                .owner(UserResponse.fromEntity(soul.getOwner()))
                .statsDownloads(soul.getStatsDownloads())
                .statsStars(soul.getStatsStars())
                .createdAt(soul.getCreatedAt())
                .build();
    }
}
