package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.dto.TrustedPublisherRequest;
import clawhub.dto.TrustedPublisherResponse;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
import clawhub.service.TrustedPublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/skills/{skillSlug}/trusted-publishers")
@RequiredArgsConstructor
public class TrustedPublisherController {

    private final TrustedPublisherService trustedPublisherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrustedPublisherResponse>>> getTrustedPublishers(
            @PathVariable String skillSlug) {

        try {
            List<TrustedPublisherResponse> publishers = trustedPublisherService.getTrustedPublishers(skillSlug);
            return ResponseEntity.ok(ApiResponse.success(publishers));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TrustedPublisherResponse>> createTrustedPublisher(
            @PathVariable String skillSlug,
            @Valid @RequestBody TrustedPublisherRequest request,
            @CurrentUser User currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            TrustedPublisherResponse response = trustedPublisherService.createTrustedPublisher(
                    skillSlug, request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Trusted publisher created successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            if (e.getMessage().contains("Not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrustedPublisher(
            @PathVariable String skillSlug,
            @PathVariable Long id,
            @CurrentUser User currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        try {
            trustedPublisherService.deleteTrustedPublisher(skillSlug, id, currentUser);
            return ResponseEntity.ok(ApiResponse.success(null, "Trusted publisher deleted successfully"));
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
