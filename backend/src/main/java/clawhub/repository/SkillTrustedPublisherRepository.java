package clawhub.repository;

import clawhub.entity.Skill;
import clawhub.entity.SkillTrustedPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillTrustedPublisherRepository extends JpaRepository<SkillTrustedPublisher, Long> {

    List<SkillTrustedPublisher> findBySkill(Skill skill);

    Optional<SkillTrustedPublisher> findBySkillAndRepositoryAndEnvironment(
            Skill skill, String repository, String environment);

    boolean existsBySkillAndRepositoryAndEnvironment(
            Skill skill, String repository, String environment);
}
