package clawhub.repository;

import clawhub.entity.Skill;
import clawhub.entity.SkillPublishToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SkillPublishTokenRepository extends JpaRepository<SkillPublishToken, Long> {

    Optional<SkillPublishToken> findByTokenHash(String tokenHash);

    List<SkillPublishToken> findBySkillAndRevokedAtIsNullAndExpiresAtAfter(
            Skill skill, Instant now);
}
