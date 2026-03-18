package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.Soul;
import clawhub.entity.Star;
import clawhub.entity.User;
import clawhub.repository.SkillRepository;
import clawhub.repository.SoulRepository;
import clawhub.repository.StarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarService {

    private final StarRepository starRepository;
    private final SkillRepository skillRepository;
    private final SoulRepository soulRepository;
    private final SkillService skillService;
    private final SoulService soulService;

    @Transactional(readOnly = true)
    public Page<Star> findByUser(User user, Pageable pageable) {
        return starRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public boolean hasStarredSkill(UUID skillId, UUID userId) {
        return starRepository.existsBySkillIdAndUserId(skillId, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasStarredSoul(UUID soulId, UUID userId) {
        return starRepository.existsBySoulIdAndUserId(soulId, userId);
    }

    @Transactional(readOnly = true)
    public long countBySkill(Skill skill) {
        return starRepository.countBySkill(skill);
    }

    @Transactional(readOnly = true)
    public long countBySoul(Soul soul) {
        return starRepository.countBySoul(soul);
    }

    @Transactional
    public void starSkill(String skillSlug, User user) {
        Skill skill = skillRepository.findBySlug(skillSlug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillSlug));

        Optional<Star> existing = starRepository.findBySkillAndUser(skill, user);
        if (existing.isPresent()) {
            log.debug("User {} already starred skill {}", user.getHandle(), skillSlug);
            return;
        }

        Star star = Star.builder()
                .skill(skill)
                .user(user)
                .build();

        starRepository.save(star);
        skillService.incrementStars(skill.getId());

        log.info("User {} starred skill {}", user.getHandle(), skillSlug);
    }

    @Transactional
    public void unstarSkill(String skillSlug, User user) {
        Skill skill = skillRepository.findBySlug(skillSlug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillSlug));

        Optional<Star> existing = starRepository.findBySkillAndUser(skill, user);
        if (existing.isEmpty()) {
            log.debug("User {} has not starred skill {}", user.getHandle(), skillSlug);
            return;
        }

        starRepository.delete(existing.get());
        skillService.decrementStars(skill.getId());

        log.info("User {} unstarred skill {}", user.getHandle(), skillSlug);
    }

    @Transactional
    public void starSoul(String soulSlug, User user) {
        Soul soul = soulRepository.findBySlug(soulSlug)
                .orElseThrow(() -> new RuntimeException("Soul not found: " + soulSlug));

        Optional<Star> existing = starRepository.findBySoulAndUser(soul, user);
        if (existing.isPresent()) {
            log.debug("User {} already starred soul {}", user.getHandle(), soulSlug);
            return;
        }

        Star star = Star.builder()
                .soul(soul)
                .user(user)
                .build();

        starRepository.save(star);
        soulService.incrementStars(soul.getId());

        log.info("User {} starred soul {}", user.getHandle(), soulSlug);
    }

    @Transactional
    public void unstarSoul(String soulSlug, User user) {
        Soul soul = soulRepository.findBySlug(soulSlug)
                .orElseThrow(() -> new RuntimeException("Soul not found: " + soulSlug));

        Optional<Star> existing = starRepository.findBySoulAndUser(soul, user);
        if (existing.isEmpty()) {
            log.debug("User {} has not starred soul {}", user.getHandle(), soulSlug);
            return;
        }

        starRepository.delete(existing.get());
        soulService.decrementStars(soul.getId());

        log.info("User {} unstarred soul {}", user.getHandle(), soulSlug);
    }
}
