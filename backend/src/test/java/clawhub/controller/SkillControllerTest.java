package clawhub.controller;

import clawhub.dto.SkillResponse;
import clawhub.entity.Skill;
import clawhub.entity.User;
import clawhub.service.SkillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SkillController.class)
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SkillService skillService;

    @Autowired
    private ObjectMapper objectMapper;

    private Skill testSkill;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .handle("testuser")
                .name("Test User")
                .role(User.Role.USER)
                .build();

        testSkill = Skill.builder()
                .id(UUID.randomUUID())
                .slug("test-skill")
                .displayName("Test Skill")
                .summary("Test summary")
                .owner(testUser)
                .statsDownloads(100L)
                .statsStars(50)
                .statsVersions(5)
                .statsComments(10)
                .build();
    }

    @Test
    void shouldReturnSkillsList() throws Exception {
        // given
        Page<Skill> skillPage = new PageImpl<>(Collections.singletonList(testSkill));
        when(skillService.findPublicSkills(any(), anyBoolean())).thenReturn(skillPage);

        // when & then
        mockMvc.perform(get("/api/skills")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].slug").value("test-skill"));
    }

    @Test
    void shouldReturnSkillBySlug() throws Exception {
        // given
        when(skillService.findBySlug("test-skill")).thenReturn(Optional.of(testSkill));

        // when & then
        mockMvc.perform(get("/api/skills/test-skill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("test-skill"))
                .andExpect(jsonPath("$.data.displayName").value("Test Skill"));
    }

    @Test
    void shouldReturn404WhenSkillNotFound() throws Exception {
        // given
        when(skillService.findBySlug("non-existent")).thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/skills/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    void shouldCreateSkillSuccessfully() throws Exception {
        // given
        String requestBody = """
                {
                    "slug": "new-skill",
                    "displayName": "New Skill",
                    "summary": "New summary"
                }
                """;

        when(skillService.createSkill(any(), any(), any(), any())).thenReturn(testSkill);

        // when & then
        mockMvc.perform(post("/api/skills")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldReturn401WhenCreatingSkillWithoutAuth() throws Exception {
        // given
        String requestBody = """
                {
                    "slug": "new-skill",
                    "displayName": "New Skill"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldUpdateSkillSuccessfully() throws Exception {
        // given
        String requestBody = """
                {
                    "displayName": "Updated Skill",
                    "summary": "Updated summary"
                }
                """;

        when(skillService.updateSkill(any(), any(), any(), any())).thenReturn(testSkill);

        // when & then
        mockMvc.perform(patch("/api/skills/test-skill")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void shouldDeleteSkillSuccessfully() throws Exception {
        // given
        when(skillService.findBySlug("test-skill")).thenReturn(Optional.of(testSkill));

        // when & then
        mockMvc.perform(delete("/api/skills/test-skill")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
