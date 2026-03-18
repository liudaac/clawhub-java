package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.dto.SkillResponse;
import clawhub.entity.Skill;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
import clawhub.service.ModerationService;
import clawhub.service.SkillService;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final SkillService skillService;

    @GetMapping("/moderation/pending")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getPendingReview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser User currentUser) {
        
        if (currentUser == null || !currentUser.isModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Moderator access required"));
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("reportCount").descending());
        Page<Skill> skills = moderationService.findPendingReview(pageable);
        
        List<SkillResponse> response = skills.getContent().stream()
                .map(SkillResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, skills.getTotalElements()));
    }

    @GetMapping("/moderation/hidden")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getHiddenSkills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser User currentUser) {
        
        if (currentUser == null || !currentUser.isModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Moderator access required"));
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("hiddenAt").descending());
        Page<Skill> skills = moderationService.findHiddenSkills(pageable);
        
        List<SkillResponse> response = skills.getContent().stream()
                .map(SkillResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, skills.getTotalElements()));
    }

    @PostMapping("/skills/{id}/hide")
    public ResponseEntity<ApiResponse<SkillResponse>> hideSkill(
            @PathVariable UUID id,
            @RequestParam String reason,
            @CurrentUser User currentUser) {
        
        if (currentUser == null || !currentUser.isModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Moderator access required"));
        }
        
        try {
            Skill skill = moderationService.hideSkill(id, reason, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromEntity(skill), "Skill hidden successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/skills/{id}/unhide")
    public ResponseEntity<ApiResponse<SkillResponse>> unhideSkill(
            @PathVariable UUID id,
            @CurrentUser User currentUser) {
        
        if (currentUser == null || !currentUser.isModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Moderator access required"));
        }
        
        try {
            Skill skill = moderationService.unhideSkill(id, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromEntity(skill), "Skill unhidden successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/skills/{id}/remove")
    public ResponseEntity<ApiResponse<SkillResponse>> removeSkill(
            @PathVariable UUID id,
            @RequestParam String reason,
            @CurrentUser User currentUser) {
        
        if (currentUser == null || !currentUser.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
        }
        
        try {
            Skill skill = moderationService.removeSkill(id, reason, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromEntity(skill), "Skill removed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/skills/{id}/verdict")
    public ResponseEntity<ApiResponse<SkillResponse>> setVerdict(
            @PathVariable UUID id,
            @RequestParam String verdict,
            @RequestParam(required = false) String notes,
            @CurrentUser User currentUser) {
        
        if (currentUser == null || !currentUser.isModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Moderator access required"));
        }
        
        try {
            Skill skill = moderationService.setModerationVerdict(id, verdict, notes, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromEntity(skill), "Verdict set successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/skills/{id}/report")
    public ResponseEntity<ApiResponse<Void>> reportSkill(
            @PathVariable UUID id,
            @RequestParam String reason,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            moderationService.incrementReportCount(id);
            log.info("Skill {} reported by user {} for reason: {}", id, currentUser.getHandle(), reason);
            return ResponseEntity.ok(ApiResponse.success(null, "Skill reported successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
