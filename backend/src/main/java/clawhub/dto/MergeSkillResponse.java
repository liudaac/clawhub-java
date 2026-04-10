package clawhub.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for skill merge operation.
 */
@Data
@Builder
public class MergeSkillResponse {
    private boolean ok;
    private String sourceSlug;
    private String targetSlug;
    private String message;
}
