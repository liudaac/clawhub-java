package clawhub.dto;

import clawhub.entity.SkillVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateVersionRequest {
    
    @NotBlank(message = "Version is required")
    @Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)?(?:\\+[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)?$", 
             message = "Version must follow semantic versioning (e.g., 1.0.0, 1.0.0-beta.1)")
    private String version;
    
    @Size(max = 50, message = "Tag must be less than 50 characters")
    private String tag;
    
    @NotBlank(message = "Changelog is required")
    @Size(max = 10000, message = "Changelog must be less than 10000 characters")
    private String changelog;
    
    @NotNull(message = "Files are required")
    private List<SkillVersion.SkillFile> files;
    
    private Object parsed;
}
