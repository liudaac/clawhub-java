package clawhub.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSoulRequest {
    
    @Size(min = 1, max = 255, message = "Display name must be between 1 and 255 characters")
    private String displayName;
    
    @Size(max = 5000, message = "Summary must be less than 5000 characters")
    private String summary;
}
