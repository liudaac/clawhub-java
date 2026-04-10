package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillPublishToken;
import clawhub.repository.SkillPublishTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishTokenService {

    private final SkillPublishTokenRepository publishTokenRepository;

    @Value("${clawhub.publish-token.expiration:3600}")
    private long tokenExpirationSeconds;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    /**
     * Generate a new publish token for a skill
     */
    @Transactional
    public String generatePublishToken(
            Skill skill,
            String version,
            GitHubOidcService.OidcTokenClaims oidcClaims) {

        // Generate a secure random token
        String tokenValue = generateSecureToken();
        String tokenHash = hashToken(tokenValue);

        // Calculate expiration
        Instant expiresAt = Instant.now().plus(Duration.ofSeconds(tokenExpirationSeconds));

        SkillPublishToken token = SkillPublishToken.builder()
                .skill(skill)
                .version(version)
                .tokenHash(tokenHash)
                .provider("github-actions")
                .repository(oidcClaims.getRepository())
                .repositoryId(oidcClaims.getRepositoryId())
                .repositoryOwner(oidcClaims.getRepositoryOwner())
                .repositoryOwnerId(oidcClaims.getRepositoryOwnerId())
                .workflowFilename(oidcClaims.getWorkflow())
                .environment(oidcClaims.getEnvironment())
                .runId(oidcClaims.getRunId())
                .runAttempt(oidcClaims.getRunAttempt())
                .sha(oidcClaims.getSha())
                .ref(oidcClaims.getRef())
                .refType(oidcClaims.getRefType())
                .actor(oidcClaims.getActor())
                .actorId(oidcClaims.getActorId())
                .expiresAt(expiresAt)
                .build();

        publishTokenRepository.save(token);

        log.info("Generated publish token for skill {} from repository {} (run {})",
                skill.getSlug(), oidcClaims.getRepository(), oidcClaims.getRunId());

        // Return the plain token (this is the only time it's available)
        return tokenValue;
    }

    /**
     * Validate a publish token and return the associated entity
     */
    @Transactional
    public Optional<SkillPublishToken> validateAndGetToken(String tokenValue) {
        String tokenHash = hashToken(tokenValue);

        Optional<SkillPublishToken> tokenOpt = publishTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            log.warn("Publish token not found");
            return Optional.empty();
        }

        SkillPublishToken token = tokenOpt.get();

        if (!token.isValid()) {
            log.warn("Publish token is invalid (expired or revoked)");
            return Optional.empty();
        }

        // Update last used timestamp
        token.setLastUsedAt(Instant.now());
        publishTokenRepository.save(token);

        return Optional.of(token);
    }

    /**
     * Revoke a publish token
     */
    @Transactional
    public void revokeToken(String tokenValue) {
        String tokenHash = hashToken(tokenValue);

        publishTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            publishTokenRepository.save(token);
            log.info("Revoked publish token for skill {}", token.getSkill().getSlug());
        });
    }

    /**
     * Revoke a publish token by ID
     */
    @Transactional
    public void revokeTokenById(Long tokenId) {
        publishTokenRepository.findById(tokenId).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            publishTokenRepository.save(token);
            log.info("Revoked publish token {}", tokenId);
        });
    }

    /**
     * Get the skill associated with a token
     */
    public Optional<Skill> getSkillFromToken(String tokenValue) {
        return validateAndGetToken(tokenValue).map(SkillPublishToken::getSkill);
    }

    /**
     * Generate a secure random token
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return "claw_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hash a token using SHA-256
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
