package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.dto.SkillResponse;
import clawhub.entity.Skill;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
import clawhub.service.BadgeService;
import clawhub.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;
    private final SkillService skillService;

    @PostMapping("/skills/{skillId}/award")
    public ResponseEntity<ApiResponse<SkillResponse>> awardBadge(
            @PathVariable UUID skillId,
            @RequestParam String badgeType,
            @CurrentUser User currentUser) {
        
        if (currentUser == null || !currentUser.isModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Moderator access required"));
        }
        
        try {
            Skill skill;
            switch (badgeType) {
                case BadgeService.BADGE_HIGHLIGHTED:
                    skill = badgeService.awardHighlightedBadge(skillId, currentUser);
                    break;
                case BadgeService.BADGE_VERIFIED:
                    skill = badgeService.awardVerifiedBadge(skillId);
                    break;
                case BadgeService.BADGE_TRENDING:
                    skill = badgeService.awardTrendingBadge(skillId);
                    break;
                case BadgeService.BADGE_STAFF_PICK:
                    skill = badgeService.awardStaffPickBadge(skillId, currentUser);
                    break;
                case BadgeService.BADGE_COMMUNITY_FAVORITE:
                    skill = badgeService.awardCommunityFavoriteBadge(skillId);
                    break;
                default:
                    skill = badgeService.addBadge(skillId, badgeType);
            }
            
            return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromEntity(skill), 
                    "Badge awarded successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/skills/{skillId}/remove")
    public ResponseEntity<ApiResponse<SkillResponse>> removeBadge(
            @PathVariable UUID skillId,
            @RequestParam String badgeType,
            @CurrentUser User currentUser) {
        
        if (currentUser == null || !currentUser.isModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Moderator access required"));
        }
        
        try {
            Skill skill = badgeService.removeBadge(skillId, badgeType);
            return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromEntity(skill), 
                    "Badge removed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/skills/{skillId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBadges(
            @PathVariable UUID skillId) {
        
        try {
            Map<String, Object> badges = badgeService.getBadges(skillId);
            return ResponseEntity.ok(ApiResponse.success(badges));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/skills/{skillId}/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkBadge(
            @PathVariable UUID skillId,
            @RequestParam String badgeType) {
        
        boolean hasBadge = badgeService.hasBadge(skillId, badgeType);
        return ResponseEntity.ok(ApiResponse.success(Map.of("hasBadge", hasBadge)));
    }

    @PostMapping("/skills/{skillId}/check-and-award")
    public ResponseEntity<ApiResponse<Void>> checkAndAwardBadges(
            @PathVariable UUID skillId,
            @CurrentUser User currentUser) {
        
        if (currentUser == null || !currentUser.isModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Moderator access required"));
        }
        
        try {
            badgeService.checkAndAwardBadges(skillId);
            return ResponseEntity.ok(ApiResponse.success(null, "Badges checked and awarded"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
