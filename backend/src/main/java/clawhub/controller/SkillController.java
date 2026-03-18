package clawhub.controller;

import clawhub.dto.*;
import clawhub.entity.Skill;
import clawhub.entity.SkillVersion;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
import clawhub.service.SkillService;
import clawhub.service.SkillVersionService;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;
    private final SkillVersionService skillVersionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillResponse>>> listSkills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "false") boolean nonSuspiciousOnly) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Skill> skills;
        if ("downloads".equalsIgnoreCase(sortBy)) {
            skills = skillService.findPublicSkillsByDownloads(pageable, nonSuspiciousOnly);
        } else if ("stars".equalsIgnoreCase(sortBy)) {
            skills = skillService.findPublicSkillsByStars(pageable, nonSuspiciousOnly);
        } else {
            skills = skillService.findPublicSkills(pageable, nonSuspiciousOnly);
        }
        
        List<SkillResponse> response = skills.getContent().stream()
                .map(SkillResponse::fromEntityMinimal)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, skills.getTotalElements()));
    }

    @GetMapping("/highlighted")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getHighlighted(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Skill> skills = skillService.findHighlighted(pageable);
        
        List<SkillResponse> response = skills.getContent().stream()
                .map(SkillResponse::fromEntityMinimal)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, skills.getTotalElements()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<SkillResponse>> getSkill(@PathVariable String slug) {
        Optional<Skill> skill = skillService.findBySlug(slug);
        
        if (skill.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Skill not found: " + slug));
        }
        
        return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromEntity(skill.get())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(
            @Valid @RequestBody CreateSkillRequest request,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            Skill skill = skillService.createSkill(
                    request.getSlug(),
                    request.getDisplayName(),
                    request.getSummary(),
                    currentUser
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(SkillResponse.fromEntity(skill), "Skill created successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{slug}")
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
            @PathVariable String slug,
            @Valid @RequestBody UpdateSkillRequest request,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            Skill skill = skillService.updateSkill(
                    slug,
                    request.getDisplayName(),
                    request.getSummary(),
                    currentUser.getId()
            );
            
            return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromEntity(skill), "Skill updated successfully"));
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

    @DeleteMapping("/{slug}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(
            @PathVariable String slug,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            skillService.deleteSkill(slug, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(null, "Skill deleted successfully"));
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

    // Version endpoints
    @GetMapping("/{slug}/versions")
    public ResponseEntity<ApiResponse<List<SkillVersionResponse>>> getVersions(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Optional<Skill> skillOpt = skillService.findBySlug(slug);
        if (skillOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Skill not found: " + slug));
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SkillVersion> versions = skillVersionService.findBySkill(skillOpt.get(), pageable);
        
        List<SkillVersionResponse> response = versions.getContent().stream()
                .map(SkillVersionResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, versions.getTotalElements()));
    }

    @GetMapping("/{slug}/versions/{version}")
    public ResponseEntity<ApiResponse<SkillVersionResponse>> getVersion(
            @PathVariable String slug,
            @PathVariable String version) {
        
        Optional<Skill> skillOpt = skillService.findBySlug(slug);
        if (skillOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Skill not found: " + slug));
        }
        
        Optional<SkillVersion> versionOpt = skillVersionService.findBySkillAndVersion(skillOpt.get(), version);
        if (versionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Version not found: " + version));
        }
        
        return ResponseEntity.ok(ApiResponse.success(SkillVersionResponse.fromEntity(versionOpt.get())));
    }

    @PostMapping("/{slug}/versions")
    public ResponseEntity<ApiResponse<SkillVersionResponse>> createVersion(
            @PathVariable String slug,
            @Valid @RequestBody CreateVersionRequest request,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            SkillVersion version = skillVersionService.createVersion(
                    slug,
                    request.getVersion(),
                    request.getTag(),
                    request.getChangelog(),
                    request.getFiles(),
                    request.getParsed(),
                    currentUser
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(SkillVersionResponse.fromEntity(version), "Version created successfully"));
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

    @PostMapping("/{slug}/rollback")
    public ResponseEntity<ApiResponse<SkillResponse>> rollbackVersion(
            @PathVariable String slug,
            @RequestParam String version,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            Skill skill = skillVersionService.rollbackToVersion(slug, version, currentUser);
            return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromEntity(skill), 
                    "Rolled back to version " + version));
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