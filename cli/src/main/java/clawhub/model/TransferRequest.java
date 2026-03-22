package clawhub.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransferRequest {
    private String id;
    private String skillSlug;
    private String fromUser;
    private String toUser;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
