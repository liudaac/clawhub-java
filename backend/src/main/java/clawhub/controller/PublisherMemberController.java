package clawhub.controller;

import clawhub.dto.PublisherMemberListResponse;
import clawhub.dto.PublisherMemberRequest;
import clawhub.dto.PublisherMemberResponse;
import clawhub.service.PublisherMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/publishers/{handle}/members")
@RequiredArgsConstructor
public class PublisherMemberController {

    private final PublisherMemberService publisherMemberService;

    @GetMapping
    public ResponseEntity<PublisherMemberListResponse> getMembers(@PathVariable String handle) {
        List<PublisherMemberResponse> members = publisherMemberService.getMembers(handle);
        return ResponseEntity.ok(PublisherMemberListResponse.builder()
                .members(members)
                .total(members.size())
                .build());
    }

    @PostMapping
    public ResponseEntity<PublisherMemberResponse> addMember(
            @PathVariable String handle,
            @Valid @RequestBody PublisherMemberRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        PublisherMemberResponse response = publisherMemberService.addMember(handle, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<PublisherMemberResponse> updateMember(
            @PathVariable String handle,
            @PathVariable UUID memberId,
            @Valid @RequestBody PublisherMemberRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        PublisherMemberResponse response = publisherMemberService.updateMember(handle, memberId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String handle,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        publisherMemberService.removeMember(handle, memberId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leavePublisher(
            @PathVariable String handle,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        publisherMemberService.leavePublisher(handle, userId);
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
