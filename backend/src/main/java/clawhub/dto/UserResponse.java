package clawhub.dto;

import clawhub.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String handle;
    private String name;
    private String bio;
    private String avatarUrl;
    private String role;
    private Instant createdAt;

    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .handle(user.getHandle())
                .name(user.getName())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name().toLowerCase())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
