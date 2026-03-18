package clawhub.model;

import lombok.Data;

@Data
public class User {
    private String id;
    private String handle;
    private String name;
    private String bio;
    private String avatarUrl;
    private String role;
    private String createdAt;
}
