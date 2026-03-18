package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.entity.Skill;
import clawhub.entity.Soul;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
import clawhub.service.SkillService;
import clawhub.service.SoulService;
import clawhub.service.StarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StarController {

    private final StarService starService;
    private final SkillService skillService;
    private final SoulService soulService;

    @PostMapping("/skills/{slug}/stars")
    public ResponseEntity<ApiResponse<Void>> starSkill(
            @PathVariable String slug,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            starService.starSkill(slug, currentUser);
            return ResponseEntity.ok(ApiResponse.success(null, "Skill starred successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/skills/{slug}/stars")
    public ResponseEntity<ApiResponse<Void>> unstarSkill(
            @PathVariable String slug,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            starService.unstarSkill(slug, currentUser);
            return ResponseEntity.ok(ApiResponse.success(null, "Skill unstarred successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/skills/{slug}/stars/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSkillStar(
            @PathVariable String slug,
            @CurrentUser User currentUser) {
        
        Optional<Skill> skillOpt = skillService.findBySlug(slug);
        if (skillOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Skill not found: " + slug));
        }
        
        Skill skill = skillOpt.get();
        boolean hasStarred = currentUser != null && 
                starService.hasStarredSkill(skill.getId(), currentUser.getId());
        long count = starService.countBySkill(skill);
        
        Map<String, Object> result = new HashMap<>();
        result.put("hasStarred", hasStarred);
        result.put("count", count);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/souls/{slug}/stars")
    public ResponseEntity<ApiResponse<Void>> starSoul(
            @PathVariable String slug,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            starService.starSoul(slug, currentUser);
            return ResponseEntity.ok(ApiResponse.success(null, "Soul starred successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/souls/{slug}/stars")
    public ResponseEntity<ApiResponse<Void>> unstarSoul(
            @PathVariable String slug,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            starService.unstarSoul(slug, currentUser);
            return ResponseEntity.ok(ApiResponse.success(null, "Soul unstarred successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/souls/{slug}/stars/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSoulStar(
            @PathVariable String slug,
            @CurrentUser User currentUser) {
        
        Optional<Soul> soulOpt = soulService.findBySlug(slug);
        if (soulOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Soul not found: " + slug));
        }
        
        Soul soul = soulOpt.get();
        boolean hasStarred = currentUser != null && 
                starService.hasStarredSoul(soul.getId(), currentUser.getId());
        long count = starService.countBySoul(soul);
        
        Map<String, Object> result = new HashMap<>();
        result.put("hasStarred", hasStarred);
        result.put("count", count);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
