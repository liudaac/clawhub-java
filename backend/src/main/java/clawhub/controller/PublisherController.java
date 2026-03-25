package clawhub.controller;

import clawhub.dto.*;
import clawhub.service.PublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/publishers")
@RequiredArgsConstructor
public class PublisherController {

    private final PublisherService publisherService;

    @PostMapping
    public ResponseEntity<PublisherResponse> createPublisher(
            @Valid @RequestBody PublisherCreateRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        PublisherResponse response = publisherService.createPublisher(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{handle}")
    public ResponseEntity<PublisherResponse> getPublisher(@PathVariable String handle) {
        PublisherResponse response = publisherService.getPublisher(handle);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PublisherListResponse> listPublishers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) Boolean trusted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PublisherListResponse response = publisherService.listPublishers(search, kind, trusted, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{handle}")
    public ResponseEntity<PublisherResponse> updatePublisher(
            @PathVariable String handle,
            @Valid @RequestBody PublisherUpdateRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        PublisherResponse response = publisherService.updatePublisher(handle, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{handle}")
    public ResponseEntity<Void> deletePublisher(
            @PathVariable String handle,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        publisherService.deletePublisher(handle, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{handle}/deactivate")
    public ResponseEntity<Void> deactivatePublisher(
            @PathVariable String handle,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        publisherService.deactivatePublisher(handle, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{handle}/reactivate")
    public ResponseEntity<Void> reactivatePublisher(
            @PathVariable String handle,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        publisherService.reactivatePublisher(handle, userId);
        return ResponseEntity.ok().build();
    }

    private UUID extractUserId(OAuth2User principal) {
        if (principal == null) {
            throw new IllegalStateException("User not authenticated");
        }
        Map<String, Object> attributes = principal.getAttributes();
        Object userId = attributes.get("id");
        if (userId instanceof String) {
            return UUID.fromString((String) userId);
        }
        throw new IllegalStateException("Could not extract user ID from principal");
    }
}
