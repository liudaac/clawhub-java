package clawhub.repository;

import clawhub.entity.Comment;
import clawhub.entity.Skill;
import clawhub.entity.Soul;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findBySkillAndDeletedAtIsNull(Skill skill, Pageable pageable);

    Page<Comment> findBySoulAndDeletedAtIsNull(Soul soul, Pageable pageable);

    java.util.List<Comment> findBySkillAndDeletedAtIsNullOrderByCreatedAtDesc(Skill skill);

    long countBySkillAndDeletedAtIsNull(Skill skill);

    long countBySoulAndDeletedAtIsNull(Soul soul);
}
