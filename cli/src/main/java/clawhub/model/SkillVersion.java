package clawhub.model;

import lombok.Data;
import java.util.List;

@Data
public class SkillVersion {
    private String id;
    private String version;
    private String tag;
    private String changelog;
    private List<SkillFile> files;
    private Object parsed;
    private User createdBy;
    private String createdAt;
}
