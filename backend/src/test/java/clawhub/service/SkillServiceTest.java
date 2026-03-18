package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.User;
import clawhub.repository.SkillRepository;
import clawhub.repository.SkillVersionRepository;
import clawhub.websocket.SkillWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillVersionRepository skillVersionRepository;

    @Mock
    private SkillWebSocketHandler webSocketHandler;

    @InjectMocks
    private SkillService skillService;

    private User testUser;
    private Skill testSkill;

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
                .statsDownloads(0L)
                .statsStars(0)
                .statsVersions(0)
                .statsComments(0)
                .build();
    }

    @Test
    void shouldCreateSkillSuccessfully() {
        // given
        when(skillRepository.existsBySlug("test-skill")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenReturn(testSkill);

        // when
        Skill result = skillService.createSkill("test-skill", "Test Skill", "Test summary", testUser);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSlug()).isEqualTo("test-skill");
        assertThat(result.getDisplayName()).isEqualTo("Test Skill");
        verify(skillRepository).save(any(Skill.class));
        verify(webSocketHandler).broadcastNewSkill(any(Skill.class));
    }

    @Test
    void shouldThrowExceptionWhenSkillSlugExists() {
        // given
        when(skillRepository.existsBySlug("test-skill")).thenReturn(true);

        // when & then
        assertThatThrownBy(() ->
                skillService.createSkill("test-skill", "Test Skill", "Test summary", testUser)
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(skillRepository, never()).save(any());
    }

    @Test
    void shouldFindSkillBySlug() {
        // given
        when(skillRepository.findBySlug("test-skill")).thenReturn(Optional.of(testSkill));

        // when
        Optional<Skill> result = skillService.findBySlug("test-skill");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSlug()).isEqualTo("test-skill");
    }

    @Test
    void shouldReturnEmptyWhenSkillNotFound() {
        // given
        when(skillRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

        // when
        Optional<Skill> result = skillService.findBySlug("non-existent");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldDeleteSkillSuccessfully() {
        // given
        when(skillRepository.findBySlug("test-skill")).thenReturn(Optional.of(testSkill));
        doNothing().when(skillRepository).delete(any(Skill.class));

        // when
        skillService.deleteSkill("test-skill", testUser.getId());

        // then
        verify(skillRepository).delete(testSkill);
    }

    @Test
    void shouldThrowExceptionWhenUnauthorizedToDelete() {
        // given
        UUID otherUserId = UUID.randomUUID();
        when(skillRepository.findBySlug("test-skill")).thenReturn(Optional.of(testSkill));

        // when & then
        assertThatThrownBy(() ->
                skillService.deleteSkill("test-skill", otherUserId)
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not authorized");

        verify(skillRepository, never()).delete(any());
    }

    @Test
    void shouldIncrementDownloads() {
        // given
        UUID skillId = testSkill.getId();
        doNothing().when(skillRepository).incrementDownloads(skillId);

        // when
        skillService.incrementDownloads(skillId);

        // then
        verify(skillRepository).incrementDownloads(skillId);
    }

    @Test
    void shouldIncrementStars() {
        // given
        UUID skillId = testSkill.getId();
        doNothing().when(skillRepository).incrementStars(skillId);

        // when
        skillService.incrementStars(skillId);

        // then
        verify(skillRepository).incrementStars(skillId);
    }
}
