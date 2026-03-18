package clawhub.repository;

import clawhub.entity.Skill;
import clawhub.entity.SkillVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillVersionRepository extends JpaRepository<SkillVersion, UUID> {

    Page<SkillVersion> findBySkillOrderByCreatedAtDesc(Skill skill, Pageable pageable);

    Optional<SkillVersion> findBySkillAndVersion(Skill skill, String version);

    boolean existsBySkillAndVersion(Skill skill, String version);

    @Query("SELECT sv FROM SkillVersion sv WHERE sv.skill = :skill AND sv.softDeletedAt IS NULL ORDER BY sv.createdAt DESC")
    Page<SkillVersion> findActiveBySkill(@Param("skill") Skill skill, Pageable pageable);

    @Query("SELECT sv FROM SkillVersion sv WHERE sv.skill = :skill AND sv.tag = :tag AND sv.softDeletedAt IS NULL")
    Optional<SkillVersion> findBySkillAndTag(@Param("skill") Skill skill, @Param("tag") String tag);

    Optional<SkillVersion> findTopBySkillOrderByCreatedAtDesc(Skill skill);

    java.util.List<SkillVersion> findBySkillOrderByCreatedAtDesc(Skill skill);
}
