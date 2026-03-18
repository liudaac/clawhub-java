package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.User;
import clawhub.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public Page<Skill> findPendingReview(Pageable pageable) {
        // Skills that need moderation review
        return skillRepository.findByModerationStatusAndReportCountGreaterThan(
                Skill.ModerationStatus.ACTIVE, 0, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Skill> findHiddenSkills(Pageable pageable) {
        return skillRepository.findByModerationStatus(Skill.ModerationStatus.HIDDEN, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Skill> findRemovedSkills(Pageable pageable) {
        return skillRepository.findByModerationStatus(Skill.ModerationStatus.REMOVED, pageable);
    }

    @Transactional
    public Skill hideSkill(UUID skillId, String reason, UUID moderatorId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        skill.setModerationStatus(Skill.ModerationStatus.HIDDEN);
        skill.setModerationReason(reason);
        skill.setHiddenAt(Instant.now());
        skill.setHiddenBy(User.builder().id(moderatorId).build());

        log.info("Skill {} hidden by moderator {}", skillId, moderatorId);
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill unhideSkill(UUID skillId, UUID moderatorId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        skill.setModerationStatus(Skill.ModerationStatus.ACTIVE);
        skill.setModerationReason(null);
        skill.setHiddenAt(null);
        skill.setHiddenBy(null);

        log.info("Skill {} unhidden by moderator {}", skillId, moderatorId);
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill removeSkill(UUID skillId, String reason, UUID moderatorId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        skill.setModerationStatus(Skill.ModerationStatus.REMOVED);
        skill.setModerationReason(reason);

        log.info("Skill {} removed by moderator {}", skillId, moderatorId);
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill setModerationVerdict(UUID skillId, String verdict, String notes, UUID moderatorId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        skill.setModerationVerdict(verdict);
        skill.setModerationNotes(notes);
        skill.setLastReviewedAt(Instant.now());

        log.info("Skill {} moderation verdict set to {} by moderator {}", skillId, verdict, moderatorId);
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill addModerationFlag(UUID skillId, String flag) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        List<String> flags = skill.getModerationFlags();
        if (!flags.contains(flag)) {
            flags.add(flag);
            skill.setModerationFlags(flags);
        }

        return skillRepository.save(skill);
    }

    @Transactional
    public Skill removeModerationFlag(UUID skillId, String flag) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        List<String> flags = skill.getModerationFlags();
        flags.remove(flag);
        skill.setModerationFlags(flags);

        return skillRepository.save(skill);
    }

    @Transactional
    public Skill incrementReportCount(UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));

        skill.setReportCount(skill.getReportCount() + 1);
        return skillRepository.save(skill);
    }

    @Transactional(readOnly = true)
    public boolean isContentFlagged(UUID skillId) {
        return skillRepository.findById(skillId)
                .map(skill -> !skill.getModerationFlags().isEmpty())
                .orElse(false);
    }
}
