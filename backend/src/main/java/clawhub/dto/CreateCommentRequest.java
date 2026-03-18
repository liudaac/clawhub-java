package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {
    
    @NotBlank(message = "Comment body is required")
    @Size(min = 1, max = 5000, message = "Comment must be between 1 and 5000 characters")
    private String body;
}
