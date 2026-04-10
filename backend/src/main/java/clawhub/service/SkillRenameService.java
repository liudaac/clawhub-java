package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillSlugAlias;
import clawhub.entity.User;
import clawhub.exception.BadRequestException;
import clawhub.exception.ForbiddenException;
import clawhub.exception.ResourceNotFoundException;
import clawhub.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for handling skill rename operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillRenameService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]*$");

    private final SkillRepository skillRepository;
    private final SkillSlugAliasService aliasService;

    /**
     * Rename a skill owned by the current user.
     *
     * @param sourceSlug  Current slug of the skill
     * @param newSlug     Desired new slug
     * @param actorUserId ID of the user performing the rename
     * @return The updated skill
     * @throws ResourceNotFoundException if skill not found
     * @throws ForbiddenException        if user doesn't own the skill
     * @throws BadRequestException       if new slug is invalid or taken
     */
    @Transactional
    public Skill renameSkill(String sourceSlug, String newSlug, UUID actorUserId) {
        // Normalize slugs
        String normalizedSourceSlug = sourceSlug.trim().toLowerCase();
        String normalizedNewSlug = newSlug.trim().toLowerCase();

        // Validate new slug format
        if (!SLUG_PATTERN.matcher(normalizedNewSlug).matches()) {
            throw new BadRequestException("Invalid slug. Use lowercase letters, numbers, and hyphens only. Must start with a letter or number.");
        }

        // Find the skill
        Skill skill = skillRepository.findBySlug(normalizedSourceSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + normalizedSourceSlug));

        // Check if skill is soft deleted
        if (skill.getSoftDeletedAt() != null) {
            throw new ResourceNotFoundException("Skill not found: " + normalizedSourceSlug);
        }

        // Check ownership
        if (!skill.getOwner().getId().equals(actorUserId)) {
            throw new ForbiddenException("Not authorized to rename this skill");
        }

        // If same slug, nothing to do
        if (skill.getSlug().equals(normalizedNewSlug)) {
            log.info("Skill {} already has slug {}, no rename needed", skill.getId(), normalizedNewSlug);
            return skill;
        }

        // Check if new slug is taken by another skill
        Optional<Skill> existingSkill = skillRepository.findBySlug(normalizedNewSlug);
        if (existingSkill.isPresent() && !existingSkill.get().getId().equals(skill.getId())) {
            Skill existing = existingSkill.get();
            if (existing.getOwner().getId().equals(actorUserId)) {
                throw new BadRequestException("Slug already belongs to one of your skills. Use merge instead.");
            } else {
                throw new BadRequestException(buildSlugTakenErrorMessage(existing));
            }
        }

        // Check if new slug is a redirect alias pointing to another skill
        Optional<SkillSlugAlias> existingAlias = aliasService.findBySlug(normalizedNewSlug);
        if (existingAlias.isPresent() && !existingAlias.get().getSkill().getId().equals(skill.getId())) {
            Skill aliasSkill = existingAlias.get().getSkill();
            throw new BadRequestException(buildAliasTakenErrorMessage(aliasSkill));
        }

        // If the new slug is an alias pointing to this skill, remove it
        if (existingAlias.isPresent() && existingAlias.get().getSkill().getId().equals(skill.getId())) {
            aliasService.deleteBySlug(normalizedNewSlug);
        }

        // Store old slug for creating redirect
        String oldSlug = skill.getSlug();

        // Update skill slug
        skill.setSlug(normalizedNewSlug);
        skill.setUpdatedAt(Instant.now());
        Skill savedSkill = skillRepository.save(skill);

        // Create redirect alias from old slug to new skill
        aliasService.createAlias(oldSlug, savedSkill, skill.getOwner());

        log.info("Renamed skill from {} to {} by user {}", oldSlug, normalizedNewSlug, actorUserId);

        return savedSkill;
    }

    /**
     * Check if a slug is available for use.
     *
     * @param slug The slug to check
     * @return true if available, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isSlugAvailable(String slug) {
        String normalizedSlug = slug.trim().toLowerCase();

        // Check format
        if (!SLUG_PATTERN.matcher(normalizedSlug).matches()) {
            return false;
        }

        // Check if taken by a skill
        if (skillRepository.existsBySlug(normalizedSlug)) {
            return false;
        }

        // Check if taken by an alias
        if (aliasService.existsBySlug(normalizedSlug)) {
            return false;
        }

        return true;
    }

    /**
     * Validate a slug format.
     *
     * @param slug The slug to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidSlugFormat(String slug) {
        return SLUG_PATTERN.matcher(slug.trim().toLowerCase()).matches();
    }

    private String buildSlugTakenErrorMessage(Skill existingSkill) {
        String ownerHandle = existingSkill.getOwner() != null ? existingSkill.getOwner().getHandle() : "unknown";
        return String.format("Slug '%s' is already taken by skill '%s' (owned by @%s). Please choose a different slug.",
                existingSkill.getSlug(),
                existingSkill.getDisplayName(),
                ownerHandle);
    }

    private String buildAliasTakenErrorMessage(Skill aliasSkill) {
        String ownerHandle = aliasSkill.getOwner() != null ? aliasSkill.getOwner().getHandle() : "unknown";
        return String.format("Slug redirects to skill '%s' (owned by @%s). Please choose a different slug.",
                aliasSkill.getDisplayName(),
                ownerHandle);
    }
}
