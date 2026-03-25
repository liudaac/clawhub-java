package clawhub.dto;

import clawhub.entity.PublisherMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherMemberResponse {

    private UUID id;
    private UUID publisherId;
    private UUID userId;
    private String userHandle;
    private String userName;
    private String userAvatarUrl;
    private String role;
    private Instant createdAt;

    public static PublisherMemberResponse fromEntity(PublisherMember member) {
        if (member == null) {
            return null;
        }
        return PublisherMemberResponse.builder()
                .id(member.getId())
                .publisherId(member.getPublisher() != null ? member.getPublisher().getId() : null)
                .userId(member.getUser() != null ? member.getUser().getId() : null)
                .userHandle(member.getUser() != null ? member.getUser().getHandle() : null)
                .userName(member.getUser() != null ? member.getUser().getName() : null)
                .userAvatarUrl(member.getUser() != null ? member.getUser().getAvatarUrl() : null)
                .role(member.getRole() != null ? member.getRole().name().toLowerCase() : null)
                .createdAt(member.getCreatedAt())
                .build();
    }
}
