package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-z0-9](?:[a-z0-9]|-(?=[a-z0-9])){0,98}$",
             message = "Name must be lowercase alphanumeric with hyphens, starting with alphanumeric")
    private String name;

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
    private String displayName;

    @Size(max = 500, message = "Summary must not exceed 500 characters")
    private String summary;

    @NotNull(message = "Family is required")
    private Family family;

    private Channel channel;

    private UUID ownerPublisherId;

    private String runtimeId;

    private Map<String, Object> compatibility;

    private Map<String, Object> capabilities;

    public enum Family {
        SKILL, CODE_PLUGIN, BUNDLE_PLUGIN
    }

    public enum Channel {
        OFFICIAL, COMMUNITY, PRIVATE
    }
}
