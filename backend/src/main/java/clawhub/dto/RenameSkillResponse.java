package clawhub.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for skill rename operation.
 */
@Data
@Builder
public class RenameSkillResponse {
    private boolean ok;
    private String slug;
    private String previousSlug;
    private String message;
}
