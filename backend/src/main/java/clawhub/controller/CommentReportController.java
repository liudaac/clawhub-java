package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.entity.CommentReport;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
import clawhub.service.CommentModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Comment Reports", description = "评论举报管理")
@SecurityRequirement(name = "bearerAuth")
public class CommentReportController {

    private final CommentModerationService commentModerationService;

    @PostMapping("/comments/{commentId}/report")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "举报评论")
    public ResponseEntity<ApiResponse<CommentReport>> reportComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody ReportRequest request,
            @CurrentUser User currentUser) {
        CommentReport report = commentModerationService.reportComment(
                commentId, request.reason(), request.details(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(summary = "获取待处理的举报列表（管理员）")
    public ResponseEntity<ApiResponse<Page<CommentReport>>> getPendingReports(
            @PageableDefault(size = 50) Pageable pageable) {
        Page<CommentReport> reports = commentModerationService.getPendingReports(pageable);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PostMapping("/admin/reports/{reportId}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(summary = "处理举报（管理员）")
    public ResponseEntity<ApiResponse<CommentReport>> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveRequest request,
            @CurrentUser User moderator) {
        CommentReport report = commentModerationService.resolveReport(
                reportId, request.status(), moderator);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // Request records
    public record ReportRequest(
            @NotBlank String reason,
            String details
    ) {}

    public record ResolveRequest(
            @NotBlank String status
    ) {
        public CommentReport.ReportStatus status() {
            return CommentReport.ReportStatus.valueOf(status.toUpperCase());
        }
    }
}
