package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillVersion;
import clawhub.entity.User;
import clawhub.repository.SkillRepository;
import clawhub.repository.SkillVersionRepository;
import clawhub.websocket.SkillWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillVersionRepository skillVersionRepository;
    private final SkillWebSocketHandler webSocketHandler;

    @Transactional(readOnly = true)
    public Page<Skill> findAll(Pageable pageable) {
        return skillRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Skill> findPublicSkills(Pageable pageable, boolean nonSuspiciousOnly) {
        return skillRepository.findPublicByCreated(nonSuspiciousOnly, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Skill> findPublicSkillsByDownloads(Pageable pageable, boolean nonSuspiciousOnly) {
        return skillRepository.findPublicByDownloads(nonSuspiciousOnly, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Skill> findPublicSkillsByStars(Pageable pageable, boolean nonSuspiciousOnly) {
        return skillRepository.findPublicByStars(nonSuspiciousOnly, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Skill> findHighlighted(Pageable pageable) {
        return skillRepository.findHighlighted(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Skill> findBySlug(String slug) {
        return skillRepository.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    public Optional<Skill> findById(UUID id) {
        return skillRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Skill> findByOwner(User owner, Pageable pageable) {
        return skillRepository.findByOwner(owner, pageable);
    }

    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return skillRepository.existsBySlug(slug);
    }

    @Transactional
    public Skill createSkill(String slug, String displayName, String summary, User owner) {
        if (skillRepository.existsBySlug(slug)) {
            throw new RuntimeException("Skill with slug '" + slug + "' already exists");
        }

        Skill skill = Skill.builder()
                .slug(slug)
                .displayName(displayName)
                .summary(summary)
                .owner(owner)
                .moderationStatus(Skill.ModerationStatus.ACTIVE)
                .statsDownloads(0L)
                .statsStars(0)
                .statsVersions(0)
                .statsComments(0)
                .build();

        Skill savedSkill = skillRepository.save(skill);
        log.info("Created skill: {} by {}", slug, owner.getHandle());
        
        // Broadcast new skill to all connected clients
        webSocketHandler.broadcastNewSkill(savedSkill);
        
        return savedSkill;
    }

    @Transactional
    public Skill updateSkill(String slug, String displayName, String summary, UUID currentUserId) {
        Skill skill = skillRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + slug));
        
        // Check ownership
        if (!skill.getOwner().getId().equals(currentUserId) && 
            !skill.getOwner().getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to update this skill");
        }
        
        if (displayName != null) {
            skill.setDisplayName(displayName);
        }
        if (summary != null) {
            skill.setSummary(summary);
        }
        
        Skill savedSkill = skillRepository.save(skill);
        log.info("Updated skill: {}", slug);
        
        // Broadcast update to subscribed clients
        webSocketHandler.broadcastSkillUpdate(savedSkill);
        
        return savedSkill;
    }

    @Transactional
    public void deleteSkill(String slug, UUID currentUserId) {
        Skill skill = skillRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + slug));
        
        // Check ownership or admin
        if (!skill.getOwner().getId().equals(currentUserId) && 
            !skill.getOwner().getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to delete this skill");
        }
        
        skillRepository.delete(skill);
        log.info("Deleted skill: {}", slug);
    }

    @Transactional
    public Skill setLatestVersion(UUID skillId, UUID versionId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));
        
        SkillVersion version = skillVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));
        
        skill.setLatestVersion(version);
        skill.setStatsVersions(skill.getStatsVersions() + 1);
        
        return skillRepository.save(skill);
    }

    @Transactional
    public void incrementDownloads(UUID skillId) {
        skillRepository.incrementDownloads(skillId);
    }

    @Transactional
    public void incrementStars(UUID skillId) {
        skillRepository.incrementStars(skillId);
    }

    @Transactional
    public void decrementStars(UUID skillId) {
        skillRepository.decrementStars(skillId);
    }

    @Transactional
    public void incrementComments(UUID skillId) {
        skillRepository.incrementComments(skillId);
    }

    @Transactional
    public void decrementComments(UUID skillId) {
        skillRepository.decrementComments(skillId);
    }

    @Transactional
    public Skill hideSkill(String slug, String reason, UUID moderatorId) {
        Skill skill = skillRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + slug));
        
        skill.setModerationStatus(Skill.ModerationStatus.HIDDEN);
        skill.setModerationReason(reason);
        skill.setHiddenAt(Instant.now());
        skill.setHiddenBy(User.builder().id(moderatorId).build());
        
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill unhideSkill(String slug) {
        Skill skill = skillRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + slug));
        
        skill.setModerationStatus(Skill.ModerationStatus.ACTIVE);
        skill.setModerationReason(null);
        skill.setHiddenAt(null);
        skill.setHiddenBy(null);
        
        return skillRepository.save(skill);
    }
}
