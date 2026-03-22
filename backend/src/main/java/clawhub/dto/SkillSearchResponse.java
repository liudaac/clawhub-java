package clawhub.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SkillSearchResponse {
    private List<SkillSearchItem> items;
    private String nextCursor;
    private boolean hasMore;
    private long total;

    @Data
    @Builder
    public static class SkillSearchItem {
        private String slug;
        private String displayName;
        private String summary;
        private String ownerHandle;
        private String ownerAvatarUrl;
        private String latestVersion;
        private Instant versionCreatedAt;
        private String changelog;
        private long downloads;
        private int stars;
        private List<String> tags;
        private Map<String, Object> badges;
        private String moderationVerdict;
        private boolean highlighted;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
