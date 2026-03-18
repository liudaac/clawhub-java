package clawhub.repository;

import clawhub.entity.Skill;
import clawhub.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SkillRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SkillRepository skillRepository;

    @Test
    void shouldFindSkillBySlug() {
        // given
        User owner = createTestUser("testuser");
        entityManager.persist(owner);

        Skill skill = Skill.builder()
                .slug("test-skill")
                .displayName("Test Skill")
                .summary("Test summary")
                .owner(owner)
                .build();
        entityManager.persist(skill);
        entityManager.flush();

        // when
        Optional<Skill> found = skillRepository.findBySlug("test-skill");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSlug()).isEqualTo("test-skill");
    }

    @Test
    void shouldCheckIfSlugExists() {
        // given
        User owner = createTestUser("testuser2");
        entityManager.persist(owner);

        Skill skill = Skill.builder()
                .slug("existing-skill")
                .displayName("Existing Skill")
                .owner(owner)
                .build();
        entityManager.persist(skill);
        entityManager.flush();

        // when
        boolean exists = skillRepository.existsBySlug("existing-skill");
        boolean notExists = skillRepository.existsBySlug("non-existent");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldFindSkillsByOwner() {
        // given
        User owner = createTestUser("owner");
        entityManager.persist(owner);

        Skill skill1 = Skill.builder()
                .slug("skill-1")
                .displayName("Skill 1")
                .owner(owner)
                .build();
        Skill skill2 = Skill.builder()
                .slug("skill-2")
                .displayName("Skill 2")
                .owner(owner)
                .build();
        entityManager.persist(skill1);
        entityManager.persist(skill2);
        entityManager.flush();

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Skill> skills = skillRepository.findByOwner(owner, pageable);

        // then
        assertThat(skills.getContent()).hasSize(2);
    }

    @Test
    void shouldFindPublicSkills() {
        // given
        User owner = createTestUser("public-owner");
        entityManager.persist(owner);

        Skill skill = Skill.builder()
                .slug("public-skill")
                .displayName("Public Skill")
                .owner(owner)
                .moderationStatus(Skill.ModerationStatus.ACTIVE)
                .build();
        entityManager.persist(skill);
        entityManager.flush();

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Skill> skills = skillRepository.findPublicByCreated(false, pageable);

        // then
        assertThat(skills.getContent()).hasSize(1);
    }

    private User createTestUser(String handle) {
        return User.builder()
                .githubId(12345L)
                .handle(handle)
                .name("Test User")
                .role(User.Role.USER)
                .build();
    }
}
