package clawhub.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-32-chars-long";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Use reflection to set the secret for testing
        try {
            java.lang.reflect.Field field = JwtService.class.getDeclaredField("jwtSecret");
            field.setAccessible(true);
            field.set(jwtService, TEST_SECRET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldGenerateValidToken() {
        // given
        UUID userId = UUID.randomUUID();
        String handle = "testuser";
        String role = "USER";

        // when
        String token = jwtService.generateToken(userId, handle, role);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void shouldExtractUsernameFromToken() {
        // given
        UUID userId = UUID.randomUUID();
        String handle = "testuser";
        String role = "USER";
        String token = jwtService.generateToken(userId, handle, role);

        // when
        String extractedHandle = jwtService.extractUsername(token);

        // then
        assertThat(extractedHandle).isEqualTo(handle);
    }

    @Test
    void shouldExtractUserIdFromToken() {
        // given
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "testuser", "USER");

        // when
        UUID extractedUserId = jwtService.extractUserId(token);

        // then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void shouldValidateToken() {
        // given
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "testuser", "USER");

        // when
        boolean isValid = jwtService.isTokenValid(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldExtractRoleFromToken() {
        // given
        String token = jwtService.generateToken(UUID.randomUUID(), "testuser", "ADMIN");

        // when
        String role = jwtService.extractRole(token);

        // then
        assertThat(role).isEqualTo("ADMIN");
    }
}
