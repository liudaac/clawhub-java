package clawhub.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SkillVersionInfo {
    private String version;
    private String changelog;
    private LocalDateTime createdAt;
    private int downloads;
}
