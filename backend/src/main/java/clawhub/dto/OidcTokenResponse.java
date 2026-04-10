package clawhub.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class OidcTokenResponse {

    private String token;
    private Instant expiresAt;
    private String skillSlug;
    private String repository;
    private String message;
}
