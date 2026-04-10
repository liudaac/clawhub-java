package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OidcTokenRequest {

    @NotBlank(message = "OIDC token is required")
    private String oidcToken;

    private String version;
}
