package clawhub.service;

import clawhub.entity.Comment;
import clawhub.entity.CommentReport;
import clawhub.entity.User;
import clawhub.exception.ResourceNotFoundException;
import clawhub.repository.CommentReportRepository;
import clawhub.repository.CommentRepository;
import clawhub.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentModerationService {

    private final CommentReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1}")
    private String apiUrl;

    @Value("${openai.model:gpt-5-mini}")
    private String model;

    @Value("${comment.moderation.enabled:false}")
    private boolean enabled;

    private static final int MAX_ACTIVE_REPORTS_PER_USER = 5;
    private static final int AUTO_HIDE_THRESHOLD = 4;

    /**
     * 举报评论
     */
    @Transactional
    public CommentReport reportComment(UUID commentId, String reason, String details, User reporter) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        // 检查是否已举报
        if (reportRepository.existsByCommentAndReporterAndStatusIn(comment, reporter,
                List.of(CommentReport.ReportStatus.PENDING, CommentReport.ReportStatus.VALID))) {
            throw new IllegalStateException("You have already reported this comment");
        }

        // 检查举报上限
        long activeReports = reportRepository.countPendingByReporter(reporter);
        if (activeReports >= MAX_ACTIVE_REPORTS_PER_USER) {
            throw new IllegalStateException("You have reached the maximum number of active reports");
        }

        CommentReport report = CommentReport.builder()
                .comment(comment)
                .reporter(reporter)
                .reason(reason)
                .details(details)
                .status(CommentReport.ReportStatus.PENDING)
                .build();

        CommentReport saved = reportRepository.save(report);
        log.info("Comment reported: {} by {} for reason: {}", commentId, reporter.getHandle(), reason);

        // 异步进行 AI 审核
        if (enabled) {
            analyzeReportAsync(saved);
        }

        // 检查是否达到自动隐藏阈值
        checkAutoHide(comment);

        return saved;
    }

    /**
     * AI 分析举报内容
     */
    @CircuitBreaker(name = "openai", fallbackMethod = "analyzeFallback")
    private void analyzeReportAsync(CommentReport report) {
        try {
            String prompt = buildScamDetectionPrompt(report);

            Map<String, Object> request = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SCAM_DETECTION_PROMPT),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object")
            );

            var response = webClientBuilder.build()
                    .post()
                    .uri(apiUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null) {
                parseAndApplyVerdict(report, response);
            }
        } catch (Exception e) {
            log.error("Failed to analyze comment report", e);
        }
    }

    private String buildScamDetectionPrompt(CommentReport report) {
        Comment comment = report.getComment();
        return String.format("""
            Analyze this comment for scam/spam content.
            
            Comment by %s:
            %s
            
            Report reason: %s
            Report details: %s
            """,
                comment.getUser().getHandle(),
                comment.getBody(),
                report.getReason(),
                report.getDetails() != null ? report.getDetails() : "N/A"
        );
    }

    private void parseAndApplyVerdict(CommentReport report, Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return;

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            JsonNode root = objectMapper.readTree(content);

            CommentReport.AiVerdict verdict = CommentReport.AiVerdict.builder()
                    .verdict(root.path("verdict").asText("ok"))
                    .confidence(root.path("confidence").asText("low"))
                    .explanation(root.path("explanation").asText(""))
                    .model(model)
                    .analyzedAt(Instant.now())
                    .build();

            report.setAiVerdict(verdict);

            // 自动处理确定的诈骗内容
            if ("certain_scam".equals(verdict.getVerdict()) && "high".equals(verdict.getConfidence())) {
                report.setStatus(CommentReport.ReportStatus.VALID);
                autoBanUser(report.getComment().getUser());
            }

            reportRepository.save(report);

        } catch (Exception e) {
            log.error("Failed to parse AI verdict", e);
        }
    }

    private void autoBanUser(User user) {
        // 保护管理员/版主账号
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.MODERATOR) {
            log.warn("Auto-ban prevented for protected user: {}", user.getHandle());
            return;
        }

        user.setDeletedAt(Instant.now());
        user.setBanReason("Automatic ban: confirmed scam comment");
        userRepository.save(user);

        log.info("User auto-banned for scam comment: {}", user.getHandle());
    }

    /**
     * 检查是否达到自动隐藏阈值
     */
    @Transactional
    public void checkAutoHide(Comment comment) {
        long reportCount = reportRepository.countActiveByComment(comment);
        if (reportCount >= AUTO_HIDE_THRESHOLD) {
            comment.setDeletedAt(Instant.now());
            commentRepository.save(comment);
            log.info("Comment auto-hidden after {} reports: {}", reportCount, comment.getId());

            // 更新所有待处理举报为自动隐藏
            List<CommentReport> pendingReports = reportRepository.findPendingByComment(comment);
            for (CommentReport report : pendingReports) {
                report.setStatus(CommentReport.ReportStatus.AUTO_HIDDEN);
                reportRepository.save(report);
            }
        }
    }

    /**
     * 处理举报
     */
    @Transactional
    public CommentReport resolveReport(UUID reportId, CommentReport.ReportStatus status, User moderator) {
        CommentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        report.setStatus(status);
        report.setResolvedAt(Instant.now());
        report.setResolvedBy(moderator);

        if (status == CommentReport.ReportStatus.VALID) {
            // 隐藏评论
            Comment comment = report.getComment();
            comment.setDeletedAt(Instant.now());
            commentRepository.save(comment);
        }

        return reportRepository.save(report);
    }

    /**
     * 获取待处理的举报
     */
    @Transactional(readOnly = true)
    public Page<CommentReport> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatus(CommentReport.ReportStatus.PENDING, pageable);
    }

    // Fallback
    private void analyzeFallback(CommentReport report, Exception ex) {
        log.warn("AI analysis fallback triggered for report {}", report.getId());
    }

    private static final String SCAM_DETECTION_PROMPT = """
You are a content moderator analyzing comments for scam, spam, or harassment.

Classify the content into one of these categories:
- **scam**: Attempts to deceive users (fake support, phishing, investment scams)
- **spam**: Unwanted promotional content
- **harassment**: Targeted abuse or threats
- **ok**: Normal content

Also provide confidence level: high, medium, low

Response format (JSON):
{
  "verdict": "scam|spam|harassment|ok",
  "confidence": "high|medium|low",
  "explanation": "Brief explanation of the decision"
}

Be conservative. Only flag clear violations.
""";
}