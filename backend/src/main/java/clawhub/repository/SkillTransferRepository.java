package clawhub.repository;

import clawhub.entity.Skill;
import clawhub.entity.SkillTransfer;
import clawhub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillTransferRepository extends JpaRepository<SkillTransfer, UUID> {

    Optional<SkillTransfer> findById(UUID id);

    List<SkillTransfer> findBySkill(Skill skill);

    @Query("SELECT st FROM SkillTransfer st WHERE st.skill = :skill AND st.status = 'PENDING' AND (st.expiresAt IS NULL OR st.expiresAt > :now)")
    Optional<SkillTransfer> findActiveBySkill(@Param("skill") Skill skill, @Param("now") Instant now);

    Page<SkillTransfer> findByFromUser(User fromUser, Pageable pageable);

    Page<SkillTransfer> findByToUser(User toUser, Pageable pageable);

    @Query("SELECT st FROM SkillTransfer st WHERE st.fromUser = :user OR st.toUser = :user")
    Page<SkillTransfer> findByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT st FROM SkillTransfer st WHERE st.toUser = :user AND st.status = 'PENDING' AND (st.expiresAt IS NULL OR st.expiresAt > :now)")
    Page<SkillTransfer> findPendingIncoming(@Param("user") User user, @Param("now") Instant now, Pageable pageable);

    @Query("SELECT st FROM SkillTransfer st WHERE st.fromUser = :user AND st.status = 'PENDING' AND (st.expiresAt IS NULL OR st.expiresAt > :now)")
    Page<SkillTransfer> findPendingOutgoing(@Param("user") User user, @Param("now") Instant now, Pageable pageable);

    @Query("SELECT COUNT(st) FROM SkillTransfer st WHERE st.skill = :skill AND st.status = 'PENDING' AND (st.expiresAt IS NULL OR st.expiresAt > :now)")
    long countActiveBySkill(@Param("skill") Skill skill, @Param("now") Instant now);

    @Query("SELECT st FROM SkillTransfer st WHERE st.status = 'PENDING' AND st.expiresAt < :now")
    List<SkillTransfer> findExpired(@Param("now") Instant now);

    boolean existsBySkillAndToUserAndStatus(Skill skill, User toUser, SkillTransfer.TransferStatus status);
}
