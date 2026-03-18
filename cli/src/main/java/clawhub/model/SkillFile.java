package clawhub.model;

import lombok.Data;

@Data
public class SkillFile {
    private String path;
    private long size;
    private String storageId;
    private String sha256;
}
