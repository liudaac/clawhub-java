package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillSlugAlias;
import clawhub.entity.User;
import clawhub.repository.SkillSlugAliasRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing skill slug aliases (redirects).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillSlugAliasService {

    private final SkillSlugAliasRepository aliasRepository;

    /**
     * Find alias by slug.
     */
    @Transactional(readOnly = true)
    public Optional<SkillSlugAlias> findBySlug(String slug) {
        return aliasRepository.findBySlugIgnoreCase(slug.trim().toLowerCase());
    }

    /**
     * Find all aliases for a skill.
     */
    @Transactional(readOnly = true)
    public List<SkillSlugAlias> findBySkill(Skill skill) {
        return aliasRepository.findBySkill(skill);
    }

    /**
     * Find all aliases for a skill ID.
     */
    @Transactional(readOnly = true)
    public List<SkillSlugAlias> findBySkillId(UUID skillId) {
        return aliasRepository.findBySkillId(skillId);
    }

    /**
     * Check if a slug is used as an alias.
     */
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return aliasRepository.existsBySlugIgnoreCase(slug.trim().toLowerCase());
    }

    /**
     * Create a new slug alias.
     */
    @Transactional
    public SkillSlugAlias createAlias(String slug, Skill skill, User owner) {
        String normalizedSlug = slug.trim().toLowerCase();

        // Check if alias already exists
        Optional<SkillSlugAlias> existing = aliasRepository.findBySlugIgnoreCase(normalizedSlug);
        if (existing.isPresent()) {
            // Update existing alias
            SkillSlugAlias alias = existing.get();
            alias.setSkill(skill);
            alias.setOwner(owner);
            alias.setUpdatedAt(Instant.now());
            log.info("Updated slug alias: {} -> skill {}", normalizedSlug, skill.getSlug());
            return aliasRepository.save(alias);
        }

        // Create new alias
        SkillSlugAlias alias = SkillSlugAlias.builder()
                .slug(normalizedSlug)
                .skill(skill)
                .owner(owner)
                .build();

        SkillSlugAlias saved = aliasRepository.save(alias);
        log.info("Created slug alias: {} -> skill {}", normalizedSlug, skill.getSlug());
        return saved;
    }

    /**
     * Delete an alias by slug.
     */
    @Transactional
    public void deleteBySlug(String slug) {
        aliasRepository.deleteBySlugIgnoreCase(slug.trim().toLowerCase());
        log.info("Deleted slug alias: {}", slug);
    }

    /**
     * Delete all aliases for a skill.
     */
    @Transactional
    public void deleteBySkillId(UUID skillId) {
        aliasRepository.deleteBySkillId(skillId);
        log.info("Deleted all aliases for skill: {}", skillId);
    }

    /**
     * Update skill reference for all aliases (used during merge).
     */
    @Transactional
    public void updateSkillForAliases(UUID oldSkillId, UUID newSkillId, UUID newOwnerId) {
        aliasRepository.updateSkillIdForAliases(oldSkillId, newSkillId, newOwnerId);
        log.info("Updated aliases from skill {} to skill {}", oldSkillId, newSkillId);
    }

    /**
     * Resolve a slug to a skill, checking both skills table and aliases.
     * Returns the target skill if found via alias, or the skill itself.
     */
    @Transactional(readOnly = true)
    public Optional<Skill> resolveSkillBySlug(String slug) {
        String normalizedSlug = slug.trim().toLowerCase();

        // First check if there's an alias
        Optional<SkillSlugAlias> alias = aliasRepository.findBySlugIgnoreCase(normalizedSlug);
        if (alias.isPresent()) {
            return Optional.of(alias.get().getSkill());
        }

        return Optional.empty();
    }
}
