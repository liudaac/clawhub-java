package clawhub.repository;

import clawhub.entity.Skill;
import clawhub.entity.Soul;
import clawhub.entity.Star;
import clawhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StarRepository extends JpaRepository<Star, UUID> {

    Optional<Star> findBySkillAndUser(Skill skill, User user);

    Optional<Star> findBySoulAndUser(Soul soul, User user);

    boolean existsBySkillAndUser(Skill skill, User user);

    boolean existsBySoulAndUser(Soul soul, User user);

    boolean existsBySkillIdAndUserId(UUID skillId, UUID userId);

    boolean existsBySoulIdAndUserId(UUID soulId, UUID userId);

    long countBySkill(Skill skill);

    long countBySoul(Soul soul);

    void deleteBySkillAndUser(Skill skill, User user);

    void deleteBySoulAndUser(Soul soul, User user);

    org.springframework.data.domain.Page<Star> findByUser(User user, org.springframework.data.domain.Pageable pageable);
}
