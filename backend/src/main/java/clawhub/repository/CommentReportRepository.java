package clawhub.repository;

import clawhub.entity.Comment;
import clawhub.entity.CommentReport;
import clawhub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, UUID> {

    Optional<CommentReport> findById(UUID id);

    Page<CommentReport> findByComment(Comment comment, Pageable pageable);

    List<CommentReport> findByCommentAndStatus(Comment comment, CommentReport.ReportStatus status);

    @Query("SELECT cr FROM CommentReport cr WHERE cr.comment = :comment AND cr.status = 'PENDING'")
    List<CommentReport> findPendingByComment(@Param("comment") Comment comment);

    @Query("SELECT COUNT(cr) FROM CommentReport cr WHERE cr.comment = :comment AND cr.status IN ('PENDING', 'VALID', 'AUTO_HIDDEN')")
    long countActiveByComment(@Param("comment") Comment comment);

    @Query("SELECT COUNT(cr) FROM CommentReport cr WHERE cr.comment = :comment AND cr.reporter = :reporter AND cr.status IN ('PENDING', 'VALID', 'AUTO_HIDDEN')")
    long countByCommentAndReporter(@Param("comment") Comment comment, @Param("reporter") User reporter);

    @Query("SELECT COUNT(cr) FROM CommentReport cr WHERE cr.reporter = :reporter AND cr.status = 'PENDING'")
    long countPendingByReporter(@Param("reporter") User reporter);

    Page<CommentReport> findByStatus(CommentReport.ReportStatus status, Pageable pageable);

    Page<CommentReport> findByReporter(User reporter, Pageable pageable);

    @Query("SELECT cr FROM CommentReport cr WHERE cr.comment.id IN :commentIds AND cr.status IN ('PENDING', 'VALID', 'AUTO_HIDDEN')")
    List<CommentReport> findActiveByCommentIds(@Param("commentIds") List<UUID> commentIds);

    boolean existsByCommentAndReporterAndStatusIn(Comment comment, User reporter, List<CommentReport.ReportStatus> statuses);
}
