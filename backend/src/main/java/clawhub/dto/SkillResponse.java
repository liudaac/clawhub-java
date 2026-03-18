package clawhub.dto;

import clawhub.entity.Skill;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillResponse {
    private UUID id;
    private String slug;
    private String displayName;
    private String summary;
    private UserResponse owner;
    private SkillVersionResponse latestVersion;
    private String resourceId;
    private SkillResponse canonicalSkill;
    private Map<String, Object> forkOf;
    private Map<String, Object> badges;
    private String moderationStatus;
    private Long statsDownloads;
    private Integer statsStars;
    private Integer statsVersions;
    private Integer statsComments;
    private Instant createdAt;
    private Instant updatedAt;

    public static SkillResponse fromEntity(Skill skill) {
        if (skill == null) return null;
        return SkillResponse.builder()
                .id(skill.getId())
                .slug(skill.getSlug())
                .displayName(skill.getDisplayName())
                .summary(skill.getSummary())
                .owner(UserResponse.fromEntity(skill.getOwner()))
                .latestVersion(skill.getLatestVersion() != null ? 
                        SkillVersionResponse.fromEntity(skill.getLatestVersion()) : null)
                .resourceId(skill.getResourceId())
                .canonicalSkill(skill.getCanonicalSkill() != null ? 
                        SkillResponse.builder()
                                .id(skill.getCanonicalSkill().getId())
                                .slug(skill.getCanonicalSkill().getSlug())
                                .displayName(skill.getCanonicalSkill().getDisplayName())
                                .build() : null)
                .forkOf(skill.getForkOf())
                .badges(skill.getBadges())
                .moderationStatus(skill.getModerationStatus().name().toLowerCase())
                .statsDownloads(skill.getStatsDownloads())
                .statsStars(skill.getStatsStars())
                .statsVersions(skill.getStatsVersions())
                .statsComments(skill.getStatsComments())
                .createdAt(skill.getCreatedAt())
                .updatedAt(skill.getUpdatedAt())
                .build();
    }

    public static SkillResponse fromEntityMinimal(Skill skill) {
        if (skill == null) return null;
        return SkillResponse.builder()
                .id(skill.getId())
                .slug(skill.getSlug())
                .displayName(skill.getDisplayName())
                .summary(skill.getSummary())
                .owner(UserResponse.fromEntity(skill.getOwner()))
                .statsDownloads(skill.getStatsDownloads())
                .statsStars(skill.getStatsStars())
                .createdAt(skill.getCreatedAt())
                .build();
    }
}
