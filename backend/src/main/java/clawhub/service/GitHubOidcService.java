package clawhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubOidcService {

    private static final String GITHUB_ACTIONS_ISSUER = "https://token.actions.githubusercontent.com";
    private static final String GITHUB_ACTIONS_JWKS_URL = GITHUB_ACTIONS_ISSUER + "/.well-known/jwks";
    private static final String TRUSTED_AUDIENCE = "clawhub";

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    // Cache for JWKS keys
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile Instant keysLastFetched = Instant.MIN;
    private static final Duration KEY_CACHE_DURATION = Duration.ofHours(1);

    @Data
    @Builder
    public static class OidcTokenClaims {
        private String sub;
        private String repository;
        private String repositoryId;
        private String repositoryOwner;
        private String repositoryOwnerId;
        private String workflow;
        private String workflowRef;
        private String workflowSha;
        private String environment;
        private String ref;
        private String sha;
        private String runId;
        private String runAttempt;
        private String runNumber;
        private String actor;
        private String actorId;
        private String refType;
        private String eventName;
        private String jobWorkflowRef;
        private String jobWorkflowSha;
        private Instant issuedAt;
        private Instant expiresAt;
    }

    /**
     * Verify and parse a GitHub OIDC token
     */
    public Optional<OidcTokenClaims> verifyOidcToken(String token) {
        try {
            // Parse the token header to get the key ID
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT format");
                return Optional.empty();
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.get("kid").asText();

            // Get the public key for this key ID
            PublicKey publicKey = getPublicKey(kid);
            if (publicKey == null) {
                log.warn("No public key found for kid: {}", kid);
                return Optional.empty();
            }

            // Verify the token
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .requireIssuer(GITHUB_ACTIONS_ISSUER)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = jws.getBody();

            // Verify audience
            String audience = claims.get("aud", String.class);
            if (!TRUSTED_AUDIENCE.equals(audience)) {
                log.warn("Invalid audience: {}", audience);
                return Optional.empty();
            }

            // Build and return the claims
            return Optional.of(OidcTokenClaims.builder()
                    .sub(claims.getSubject())
                    .repository(claims.get("repository", String.class))
                    .repositoryId(claims.get("repository_id", String.class))
                    .repositoryOwner(claims.get("repository_owner", String.class))
                    .repositoryOwnerId(claims.get("repository_owner_id", String.class))
                    .workflow(claims.get("workflow", String.class))
                    .workflowRef(claims.get("workflow_ref", String.class))
                    .workflowSha(claims.get("workflow_sha", String.class))
                    .environment(claims.get("environment", String.class))
                    .ref(claims.get("ref", String.class))
                    .sha(claims.get("sha", String.class))
                    .runId(claims.get("run_id", String.class))
                    .runAttempt(claims.get("run_attempt", String.class))
                    .runNumber(claims.get("run_number", String.class))
                    .actor(claims.get("actor", String.class))
                    .actorId(claims.get("actor_id", String.class))
                    .refType(claims.get("ref_type", String.class))
                    .eventName(claims.get("event_name", String.class))
                    .jobWorkflowRef(claims.get("job_workflow_ref", String.class))
                    .jobWorkflowSha(claims.get("job_workflow_sha", String.class))
                    .issuedAt(claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : null)
                    .expiresAt(claims.getExpiration() != null ? claims.getExpiration().toInstant() : null)
                    .build());

        } catch (JwtException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error verifying OIDC token", e);
            return Optional.empty();
        }
    }

    /**
     * Get public key from cache or fetch from GitHub JWKS endpoint
     */
    private PublicKey getPublicKey(String kid) {
        // Check if cache needs refresh
        if (keyCache.isEmpty() || Instant.now().isAfter(keysLastFetched.plus(KEY_CACHE_DURATION))) {
            refreshJwksKeys();
        }
        return keyCache.get(kid);
    }

    /**
     * Fetch JWKS keys from GitHub
     */
    @Cacheable(value = "githubJwks", unless = "#result == null")
    private synchronized void refreshJwksKeys() {
        try {
            log.info("Refreshing GitHub JWKS keys");

            String jwksJson = webClientBuilder.build()
                    .get()
                    .uri(GITHUB_ACTIONS_JWKS_URL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (jwksJson == null) {
                log.warn("Empty JWKS response");
                return;
            }

            JsonNode jwks = objectMapper.readTree(jwksJson);
            JsonNode keys = jwks.get("keys");

            if (keys == null || !keys.isArray()) {
                log.warn("Invalid JWKS format");
                return;
            }

            Map<String, PublicKey> newKeys = new ConcurrentHashMap<>();

            for (JsonNode key : keys) {
                String kid = key.get("kid").asText();
                String kty = key.get("kty").asText();

                if (!"RSA".equals(kty)) {
                    log.debug("Skipping non-RSA key: {}", kid);
                    continue;
                }

                String n = key.get("n").asText();
                String e = key.get("e").asText();

                PublicKey publicKey = buildRsaPublicKey(n, e);
                if (publicKey != null) {
                    newKeys.put(kid, publicKey);
                }
            }

            keyCache.clear();
            keyCache.putAll(newKeys);
            keysLastFetched = Instant.now();

            log.info("Successfully cached {} JWKS keys", keyCache.size());

        } catch (Exception ex) {
            log.error("Failed to fetch JWKS keys", ex);
        }
    }

    /**
     * Build RSA public key from base64-encoded modulus and exponent
     */
    private PublicKey buildRsaPublicKey(String modulusBase64, String exponentBase64) {
        try {
            byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusBase64);
            byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentBase64);

            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);

        } catch (Exception e) {
            log.error("Failed to build RSA public key", e);
            return null;
        }
    }

    /**
     * Extract workflow filename from workflow_ref claim
     * Example: owner/repo/.github/workflows/main.yml@refs/heads/main
     */
    public String extractWorkflowFilename(String workflowRef) {
        if (workflowRef == null || workflowRef.isEmpty()) {
            return null;
        }

        // Remove the @ref suffix if present
        int atIndex = workflowRef.lastIndexOf('@');
        if (atIndex > 0) {
            workflowRef = workflowRef.substring(0, atIndex);
        }

        // Extract the filename from the path
        int lastSlash = workflowRef.lastIndexOf('/');
        if (lastSlash > 0) {
            return workflowRef.substring(lastSlash + 1);
        }

        return workflowRef;
    }
}
