package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModerationActionRequest {

    private String reason;

    @NotBlank(message = "Moderation note is required")
    private String note;
}
