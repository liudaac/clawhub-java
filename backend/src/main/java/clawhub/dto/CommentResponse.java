package clawhub.dto;

import clawhub.entity.Comment;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {
    private UUID id;
    private UUID skillId;
    private UUID soulId;
    private UserResponse user;
    private String body;
    private Instant createdAt;
    private Instant updatedAt;

    public static CommentResponse fromEntity(Comment comment) {
        if (comment == null) return null;
        return CommentResponse.builder()
                .id(comment.getId())
                .skillId(comment.getSkill() != null ? comment.getSkill().getId() : null)
                .soulId(comment.getSoul() != null ? comment.getSoul().getId() : null)
                .user(UserResponse.fromEntity(comment.getUser()))
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
