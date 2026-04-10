package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for renaming a skill.
 */
@Data
public class RenameSkillRequest {

    @NotBlank(message = "New slug is required")
    @Size(min = 1, max = 255, message = "Slug must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*$", message = "Invalid slug. Use lowercase letters, numbers, and hyphens only. Must start with a letter or number.")
    private String newSlug;
}
