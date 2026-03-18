package clawhub.controller;

import clawhub.dto.*;
import clawhub.entity.Comment;
import clawhub.entity.Skill;
import clawhub.entity.Soul;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
import clawhub.service.CommentService;
import clawhub.service.SkillService;
import clawhub.service.SoulService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final SkillService skillService;
    private final SoulService soulService;

    @GetMapping("/skills/{slug}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getSkillComments(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Optional<Skill> skillOpt = skillService.findBySlug(slug);
        if (skillOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Skill not found: " + slug));
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Comment> comments = commentService.findBySkill(skillOpt.get(), pageable);
        
        List<CommentResponse> response = comments.getContent().stream()
                .map(CommentResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, comments.getTotalElements()));
    }

    @GetMapping("/souls/{slug}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getSoulComments(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Optional<Soul> soulOpt = soulService.findBySlug(slug);
        if (soulOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Soul not found: " + slug));
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Comment> comments = commentService.findBySoul(soulOpt.get(), pageable);
        
        List<CommentResponse> response = comments.getContent().stream()
                .map(CommentResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, comments.getTotalElements()));
    }

    @PostMapping("/skills/{slug}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createSkillComment(
            @PathVariable String slug,
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            Comment comment = commentService.createSkillComment(slug, request.getBody(), currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(CommentResponse.fromEntity(comment), "Comment created successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/souls/{slug}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createSoulComment(
            @PathVariable String slug,
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            Comment comment = commentService.createSoulComment(slug, request.getBody(), currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(CommentResponse.fromEntity(comment), "Comment created successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest request,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            Comment comment = commentService.updateComment(id, request.getBody(), currentUser);
            return ResponseEntity.ok(ApiResponse.success(CommentResponse.fromEntity(comment), "Comment updated successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            if (e.getMessage().contains("Not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID id,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            commentService.deleteComment(id, currentUser);
            return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            if (e.getMessage().contains("Not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}