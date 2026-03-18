package clawhub.service;

import clawhub.dto.CommentResponse;
import clawhub.entity.Comment;
import clawhub.entity.Skill;
import clawhub.entity.Soul;
import clawhub.entity.User;
import clawhub.repository.CommentRepository;
import clawhub.repository.SkillRepository;
import clawhub.repository.SoulRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final SkillRepository skillRepository;
    private final SoulRepository soulRepository;
    private final SkillService skillService;
    private final SoulService soulService;

    @Transactional(readOnly = true)
    public Page<Comment> findBySkill(Skill skill, Pageable pageable) {
        return commentRepository.findBySkillAndDeletedAtIsNull(skill, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Comment> findBySoul(Soul soul, Pageable pageable) {
        return commentRepository.findBySoulAndDeletedAtIsNull(soul, pageable);
    }

    @Transactional(readOnly = true)
    public List<Comment> findBySkillOrderByCreatedAtDesc(Skill skill) {
        return commentRepository.findBySkillAndDeletedAtIsNullOrderByCreatedAtDesc(skill);
    }

    @Transactional(readOnly = true)
    public Optional<Comment> findById(UUID id) {
        return commentRepository.findById(id);
    }

    @Transactional
    public Comment createSkillComment(String skillSlug, String body, User user) {
        Skill skill = skillRepository.findBySlug(skillSlug)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + skillSlug));

        Comment comment = Comment.builder()
                .skill(skill)
                .user(user)
                .body(body)
                .build();

        Comment saved = commentRepository.save(comment);
        skillService.incrementComments(skill.getId());

        log.info("Created comment on skill {} by {}", skillSlug, user.getHandle());
        return saved;
    }

    @Transactional
    public Comment createSoulComment(String soulSlug, String body, User user) {
        Soul soul = soulRepository.findBySlug(soulSlug)
                .orElseThrow(() -> new RuntimeException("Soul not found: " + soulSlug));

        Comment comment = Comment.builder()
                .soul(soul)
                .user(user)
                .body(body)
                .build();

        Comment saved = commentRepository.save(comment);
        soulService.incrementComments(soul.getId());

        log.info("Created comment on soul {} by {}", soulSlug, user.getHandle());
        return saved;
    }

    @Transactional
    public Comment updateComment(UUID commentId, String body, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        if (!comment.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to update this comment");
        }

        comment.setBody(body);
        comment.setUpdatedAt(Instant.now());

        log.info("Updated comment {} by {}", commentId, currentUser.getHandle());
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(UUID commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        if (!comment.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);

        // Decrement comment count
        if (comment.getSkill() != null) {
            skillService.decrementComments(comment.getSkill().getId());
        } else if (comment.getSoul() != null) {
            soulService.decrementComments(comment.getSoul().getId());
        }

        log.info("Deleted comment {} by {}", commentId, currentUser.getHandle());
    }

    @Transactional(readOnly = true)
    public long countBySkill(Skill skill) {
        return commentRepository.countBySkillAndDeletedAtIsNull(skill);
    }

    @Transactional(readOnly = true)
    public long countBySoul(Soul soul) {
        return commentRepository.countBySoulAndDeletedAtIsNull(soul);
    }
}
