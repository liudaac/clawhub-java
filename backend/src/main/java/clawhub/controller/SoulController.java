package clawhub.controller;

import clawhub.dto.*;
import clawhub.entity.Soul;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/souls")
@RequiredArgsConstructor
public class SoulController {

    private final SoulService soulService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SoulResponse>>> listSouls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Soul> souls = soulService.findAll(pageable);
        
        List<SoulResponse> response = souls.getContent().stream()
                .map(SoulResponse::fromEntityMinimal)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, souls.getTotalElements()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<SoulResponse>> getSoul(@PathVariable String slug) {
        Optional<Soul> soul = soulService.findBySlug(slug);
        
        if (soul.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Soul not found: " + slug));
        }
        
        return ResponseEntity.ok(ApiResponse.success(SoulResponse.fromEntity(soul.get())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SoulResponse>> createSoul(
            @Valid @RequestBody CreateSoulRequest request,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            Soul soul = soulService.createSoul(
                    request.getSlug(),
                    request.getDisplayName(),
                    request.getSummary(),
                    currentUser
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(SoulResponse.fromEntity(soul), "Soul created successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{slug}")
    public ResponseEntity<ApiResponse<SoulResponse>> updateSoul(
            @PathVariable String slug,
            @Valid @RequestBody UpdateSoulRequest request,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            Soul soul = soulService.updateSoul(
                    slug,
                    request.getDisplayName(),
                    request.getSummary(),
                    currentUser.getId()
            );
            
            return ResponseEntity.ok(ApiResponse.success(SoulResponse.fromEntity(soul), "Soul updated successfully"));
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
    public ResponseEntity<ApiResponse<Void>> deleteSoul(
            @PathVariable String slug,
            @CurrentUser User currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        
        try {
            soulService.deleteSoul(slug, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(null, "Soul deleted successfully"));
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
