package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.dto.OidcTokenRequest;
import clawhub.dto.OidcTokenResponse;
import clawhub.entity.Skill;
import clawhub.entity.SkillTrustedPublisher;
import clawhub.service.GitHubOidcService;
import clawhub.service.PublishTokenService;
import clawhub.service.SkillService;
import clawhub.service.TrustedPublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth/oidc")
@RequiredArgsConstructor
public class OidcAuthController {

    private final GitHubOidcService gitHubOidcService;
    private final TrustedPublisherService trustedPublisherService;
    private final PublishTokenService publishTokenService;
    private final SkillService skillService;

    /**
     * Exchange a GitHub OIDC token for a short-lived publish token
     */
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<OidcTokenResponse>> exchangeOidcToken(
            @Valid @RequestBody OidcTokenRequest request) {

        // Verify the OIDC token
        Optional<GitHubOidcService.OidcTokenClaims> claimsOpt =
                gitHubOidcService.verifyOidcToken(request.getOidcToken());

        if (claimsOpt.isEmpty()) {
            log.warn("Invalid OIDC token provided");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid OIDC token"));
        }

        GitHubOidcService.OidcTokenClaims claims = claimsOpt.get();

        // Extract repository information
        String repository = claims.getRepository();
        String environment = claims.getEnvironment();

        if (repository == null || repository.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Repository information missing from OIDC token"));
        }

        if (environment == null || environment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Environment information missing from OIDC token"));
        }

        log.info("OIDC token exchange request from repository: {}, environment: {}, workflow: {}",
                repository, environment, claims.getWorkflow());

        // Return the claims information for the client to use in the next step
        return ResponseEntity.ok(ApiResponse.success(OidcTokenResponse.builder()
                .message("OIDC token verified. Use /api/auth/oidc/exchange/{skillSlug} to get a publish token.")
                .repository(repository)
                .build()));
    }

    /**
     * Exchange OIDC token for a publish token for a specific skill
     */
    @PostMapping("/exchange/{skillSlug}")
    public ResponseEntity<ApiResponse<OidcTokenResponse>> exchangeForSkill(
            @PathVariable String skillSlug,
            @Valid @RequestBody OidcTokenRequest request) {

        // Verify the OIDC token
        Optional<GitHubOidcService.OidcTokenClaims> claimsOpt =
                gitHubOidcService.verifyOidcToken(request.getOidcToken());

        if (claimsOpt.isEmpty()) {
            log.warn("Invalid OIDC token provided for skill: {}", skillSlug);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid OIDC token"));
        }

        GitHubOidcService.OidcTokenClaims claims = claimsOpt.get();

        // Find the skill
        Optional<Skill> skillOpt = skillService.findBySlug(skillSlug);
        if (skillOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Skill not found: " + skillSlug));
        }

        Skill skill = skillOpt.get();

        // Check if the repository is a trusted publisher for this skill
        Optional<SkillTrustedPublisher> publisherOpt =
                trustedPublisherService.findMatchingPublisher(skill, claims.getRepository(), claims.getEnvironment());

        if (publisherOpt.isEmpty()) {
            log.warn("Repository {} is not a trusted publisher for skill {}",
                    claims.getRepository(), skillSlug);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Repository is not a trusted publisher for this skill"));
        }

        SkillTrustedPublisher publisher = publisherOpt.get();

        // Verify workflow matches
        String workflowFilename = gitHubOidcService.extractWorkflowFilename(claims.getWorkflowRef());
        if (!publisher.getWorkflowFilename().equals(workflowFilename)) {
            log.warn("Workflow mismatch: expected {}, got {}",
                    publisher.getWorkflowFilename(), workflowFilename);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Workflow does not match the trusted publisher configuration"));
        }

        // Verify repository ID matches
        if (!publisher.getRepositoryId().equals(claims.getRepositoryId())) {
            log.warn("Repository ID mismatch: expected {}, got {}",
                    publisher.getRepositoryId(), claims.getRepositoryId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Repository ID does not match the trusted publisher configuration"));
        }

        // Generate a publish token
        String publishToken = publishTokenService.generatePublishToken(
                skill, request.getVersion(), claims);

        log.info("Generated publish token for skill {} from repository {} (run {})",
                skillSlug, claims.getRepository(), claims.getRunId());

        return ResponseEntity.ok(ApiResponse.success(OidcTokenResponse.builder()
                .token(publishToken)
                .expiresAt(Instant.now().plusSeconds(3600)) // 1 hour expiration
                .skillSlug(skillSlug)
                .repository(claims.getRepository())
                .message("Publish token generated successfully")
                .build()));
    }

    /**
     * Verify a publish token (for internal use by other services)
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyPublishToken(@RequestParam String token) {
        Optional<clawhub.entity.SkillPublishToken> tokenOpt = publishTokenService.validateAndGetToken(token);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }

        return ResponseEntity.ok(ApiResponse.success(true));
    }
}
