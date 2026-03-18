package clawhub.model;

import lombok.Data;
import java.util.Map;

@Data
public class Skill {
    private String id;
    private String slug;
    private String displayName;
    private String summary;
    private User owner;
    private SkillVersion latestVersion;
    private Map<String, Object> badges;
    private String moderationStatus;
    private long statsDownloads;
    private int statsStars;
    private int statsVersions;
    private int statsComments;
    private String createdAt;
    private String updatedAt;
}
