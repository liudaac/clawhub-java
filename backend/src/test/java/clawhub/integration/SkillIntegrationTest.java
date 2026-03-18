package clawhub.integration;

import clawhub.entity.Skill;
import clawhub.entity.User;
import clawhub.repository.SkillRepository;
import clawhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SkillIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clawhub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        skillRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .githubId(12345L)
                .handle("testuser")
                .name("Test User")
                .role(User.Role.USER)
                .build();
        userRepository.save(testUser);
    }

    @Test
    void shouldGetEmptySkillsList() throws Exception {
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser
    void shouldCreateAndRetrieveSkill() throws Exception {
        // Create skill
        String requestBody = """
                {
                    "slug": "test-skill",
                    "displayName": "Test Skill",
                    "summary": "Test summary"
                }
                """;

        mockMvc.perform(post("/api/skills")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("test-skill"));

        // Retrieve skill
        mockMvc.perform(get("/api/skills/test-skill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("test-skill"))
                .andExpect(jsonPath("$.data.displayName").value("Test Skill"));
    }

    @Test
    @WithMockUser
    void shouldUpdateSkill() throws Exception {
        // Create skill first
        Skill skill = Skill.builder()
                .slug("update-test")
                .displayName("Original Name")
                .summary("Original summary")
                .owner(testUser)
                .build();
        skillRepository.save(skill);

        // Update skill
        String updateBody = """
                {
                    "displayName": "Updated Name",
                    "summary": "Updated summary"
                }
                """;

        mockMvc.perform(patch("/api/skills/update-test")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Updated Name"));
    }

    @Test
    @WithMockUser
    void shouldDeleteSkill() throws Exception {
        // Create skill first
        Skill skill = Skill.builder()
                .slug("delete-test")
                .displayName("Delete Test")
                .owner(testUser)
                .build();
        skillRepository.save(skill);

        // Delete skill
        mockMvc.perform(delete("/api/skills/delete-test")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify deletion
        mockMvc.perform(get("/api/skills/delete-test"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForNonExistentSkill() throws Exception {
        mockMvc.perform(get("/api/skills/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    void shouldRejectInvalidSkillData() throws Exception {
        String invalidBody = """
                {
                    "slug": "",
                    "displayName": ""
                }
                """;

        mockMvc.perform(post("/api/skills")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
}
