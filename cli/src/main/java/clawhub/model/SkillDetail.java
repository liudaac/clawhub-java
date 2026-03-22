package clawhub.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class SkillDetail {
    private String slug;
    private String displayName;
    private String ownerHandle;
    private String latestVersion;
    private String summary;
    private String description;
    private String license;
    private long downloads;
    private int stars;
    private String status;
    private String moderationVerdict;
    private List<String> moderationFlags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SkillVersionInfo> versions;
    private List<SkillFileInfo> files;
    private Map<String, Object> metadata;
}
