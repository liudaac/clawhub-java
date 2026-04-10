package clawhub.repository;

import clawhub.entity.Skill;
import clawhub.entity.SkillSlugAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing skill slug aliases (redirects).
 */
@Repository
public interface SkillSlugAliasRepository extends JpaRepository<SkillSlugAlias, UUID> {

    /**
     * Find alias by slug (case-insensitive).
     */
    Optional<SkillSlugAlias> findBySlugIgnoreCase(String slug);

    /**
     * Find all aliases for a specific skill.
     */
    List<SkillSlugAlias> findBySkill(Skill skill);

    /**
     * Find all aliases for a specific skill ID.
     */
    @Query("SELECT a FROM SkillSlugAlias a WHERE a.skill.id = :skillId")
    List<SkillSlugAlias> findBySkillId(@Param("skillId") UUID skillId);

    /**
     * Check if a slug alias exists.
     */
    boolean existsBySlugIgnoreCase(String slug);

    /**
     * Delete all aliases for a specific skill.
     */
    @Modifying
    @Query("DELETE FROM SkillSlugAlias a WHERE a.skill.id = :skillId")
    void deleteBySkillId(@Param("skillId") UUID skillId);

    /**
     * Update skill reference for aliases (used during merge).
     */
    @Modifying
    @Query("UPDATE SkillSlugAlias a SET a.skill.id = :newSkillId, a.owner.id = :newOwnerId, a.updatedAt = CURRENT_TIMESTAMP WHERE a.skill.id = :oldSkillId")
    void updateSkillIdForAliases(@Param("oldSkillId") UUID oldSkillId, @Param("newSkillId") UUID newSkillId, @Param("newOwnerId") UUID newOwnerId);

    /**
     * Delete alias by slug.
     */
    @Modifying
    void deleteBySlugIgnoreCase(String slug);
}
