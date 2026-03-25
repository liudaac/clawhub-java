package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherCreateRequest {

    @NotNull(message = "Kind is required")
    private Kind kind;

    @NotBlank(message = "Handle is required")
    @Size(min = 2, max = 39, message = "Handle must be between 2 and 39 characters")
    @Pattern(regexp = "^[a-zA-Z0-9](?:[a-zA-Z0-9]|-(?=[a-zA-Z0-9])){0,38}$", 
             message = "Handle must start with alphanumeric and can only contain alphanumeric characters and hyphens")
    private String handle;

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    @Size(max = 1024, message = "Image URL must not exceed 1024 characters")
    private String image;

    public enum Kind {
        USER, ORG
    }
}
