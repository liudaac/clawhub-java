package clawhub.model;

import lombok.Data;

@Data
public class SkillFileInfo {
    private String path;
    private long size;
    private String sha256;
    private String contentType;
}
