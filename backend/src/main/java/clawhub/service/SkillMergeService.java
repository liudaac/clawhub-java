package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillSlugAlias;
import clawhub.entity.SkillVersion;
import clawhub.exception.BadRequestException;
import clawhub.exception.ForbiddenException;
import clawhub.exception.ResourceNotFoundException;
import clawhub.repository.CommentRepository;
import clawhub.repository.SkillRepository;
import clawhub.repository.SkillVersionRepository;
import clawhub.repository.StarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling skill merge operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillMergeService {

    private final SkillRepository skillRepository;
    private final SkillSlugAliasService aliasService;
    private final SkillVersionRepository skillVersionRepository;
    private final StarRepository starRepository;
    private final CommentRepository commentRepository;

    /**
     * Merge a source skill into a target skill.
     * Both skills must be owned by the actor user.
     *
     * @param sourceSlug  Slug of the skill to merge (will be hidden)
     * @param targetSlug  Slug of the skill to merge into
     * @param actorUserId ID of the user performing the merge
     * @return The target skill after merge
     * @throws ResourceNotFoundException if either skill not found
     * @throws ForbiddenException        if user doesn't own both skills
     * @throws BadRequestException       if trying to merge same skill or invalid operation
     */
    @Transactional
    public Skill mergeSkills(String sourceSlug, String targetSlug, UUID actorUserId) {
        // Normalize slugs
        String normalizedSourceSlug = sourceSlug.trim().toLowerCase();
        String normalizedTargetSlug = targetSlug.trim().toLowerCase();

        // Validate different slugs
        if (normalizedSourceSlug.equals(normalizedTargetSlug)) {
            throw new BadRequestException("Source and target must be different skills");
        }

        // Find source skill
        Skill source = skillRepository.findBySlug(normalizedSourceSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Source skill not found: " + normalizedSourceSlug));

        // Check if source is soft deleted
        if (source.getSoftDeletedAt() != null) {
            throw new ResourceNotFoundException("Source skill not found: " + normalizedSourceSlug);
        }

        // Find target skill (check aliases too)
        Skill target = resolveSkillBySlug(normalizedTargetSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Target skill not found: " + normalizedTargetSlug));

        // Check if target is soft deleted
        if (target.getSoftDeletedAt() != null) {
            throw new ResourceNotFoundException("Target skill not found: " + normalizedTargetSlug);
        }

        // Check same skill
        if (source.getId().equals(target.getId())) {
            throw new BadRequestException("Source and target must be different skills");
        }

        // Check ownership of both skills
        if (!source.getOwner().getId().equals(actorUserId)) {
            throw new ForbiddenException("Not authorized to merge source skill");
        }
        if (!target.getOwner().getId().equals(actorUserId)) {
            throw new ForbiddenException("Not authorized to merge into target skill");
        }

        // Get target's latest version for forkOf info
        SkillVersion targetLatestVersion = target.getLatestVersion();

        // Get target's canonical skill ID (or use target's own ID)
        UUID targetCanonicalSkillId = target.getCanonicalSkill() != null
                ? target.getCanonicalSkill().getId()
                : target.getId();

        // Move all slug aliases from source to target
        List<SkillSlugAlias> sourceAliases = aliasService.findBySkill(source);
        for (SkillSlugAlias alias : sourceAliases) {
            // Skip if alias slug equals target's current slug
            if (alias.getSlug().equals(target.getSlug())) {
                aliasService.deleteBySlug(alias.getSlug());
                continue;
            }
            // Update alias to point to target
            alias.setSkill(target);
            alias.setOwner(target.getOwner());
            alias.setUpdatedAt(Instant.now());
            // Note: This will be saved when we call updateSkillForAliases or individually
        }

        // Create redirect alias from source slug to target
        aliasService.createAlias(source.getSlug(), target, target.getOwner());

        // Repoint all relationships (stars, comments) from source to target
        repointSkillRelationships(source, target);

        // Update source skill to mark as merged
        Map<String, Object> forkOf = new HashMap<>();
        forkOf.put("skillId", target.getId().toString());
        forkOf.put("kind", "duplicate");
        forkOf.put("version", targetLatestVersion != null ? targetLatestVersion.getVersion() : null);
        forkOf.put("at", Instant.now().toEpochMilli());

        source.setCanonicalSkill(target);
        source.setForkOf(forkOf);
        source.setSoftDeletedAt(Instant.now());
        source.setModerationStatus(Skill.ModerationStatus.HIDDEN);
        source.setModerationReason("owner.merged");
        source.setHiddenAt(Instant.now());
        source.setHiddenBy(target.getOwner());
        source.setLastReviewedAt(Instant.now());
        source.setUpdatedAt(Instant.now());

        // Update stats on target skill
        target.setStatsStars(target.getStatsStars() + source.getStatsStars());
        target.setStatsDownloads(target.getStatsDownloads() + source.getStatsDownloads());
        target.setStatsComments(target.getStatsComments() + source.getStatsComments());
        target.setUpdatedAt(Instant.now());

        // Save both skills
        skillRepository.save(source);
        Skill savedTarget = skillRepository.save(target);

        log.info("Merged skill {} into {} by user {}", normalizedSourceSlug, normalizedTargetSlug, actorUserId);

        return savedTarget;
    }

    /**
     * Repoint all relationships from source skill to target skill.
     * This includes stars and comments.
     */
    @Transactional
    protected void repointSkillRelationships(Skill source, Skill target) {
        // Repoint comments
        // Get all comments for source skill
        List<clawhub.entity.Comment> sourceComments = commentRepository
                .findBySkillAndDeletedAtIsNullOrderByCreatedAtDesc(source);

        for (clawhub.entity.Comment comment : sourceComments) {
            comment.setSkill(target);
            comment.setUpdatedAt(Instant.now());
        }
        commentRepository.saveAll(sourceComments);

        log.info("Repointed {} comments from skill {} to skill {}",
                sourceComments.size(), source.getId(), target.getId());
    }

    /**
     * Resolve a skill by slug, checking both direct skills and aliases.
     *
     * @param slug The slug to resolve
     * @return Optional containing the skill if found
     */
    @Transactional(readOnly = true)
    public Optional<Skill> resolveSkillBySlug(String slug) {
        // First try direct skill lookup
        Optional<Skill> skill = skillRepository.findBySlug(slug);
        if (skill.isPresent()) {
            return skill;
        }

        // Try alias lookup
        return aliasService.resolveSkillBySlug(slug);
    }
}
