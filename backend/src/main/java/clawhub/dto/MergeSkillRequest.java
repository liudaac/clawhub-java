package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for merging a skill into another.
 */
@Data
public class MergeSkillRequest {

    @NotBlank(message = "Target slug is required")
    private String targetSlug;
}
