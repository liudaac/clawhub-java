package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSkillRequest {
    
    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens")
    @Size(min = 1, max = 255, message = "Slug must be between 1 and 255 characters")
    private String slug;
    
    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 255, message = "Display name must be between 1 and 255 characters")
    private String displayName;
    
    @Size(max = 5000, message = "Summary must be less than 5000 characters")
    private String summary;
}
