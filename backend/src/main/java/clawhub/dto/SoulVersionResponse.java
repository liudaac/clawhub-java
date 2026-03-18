package clawhub.dto;

import clawhub.entity.SoulVersion;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SoulVersionResponse {
    private UUID id;
    private String version;
    private String tag;
    private String changelog;
    private List<SoulVersion.SoulFile> files;
    private Object parsed;
    private UserResponse createdBy;
    private Instant createdAt;

    public static SoulVersionResponse fromEntity(SoulVersion version) {
        if (version == null) return null;
        return SoulVersionResponse.builder()
                .id(version.getId())
                .version(version.getVersion())
                .tag(version.getTag())
                .changelog(version.getChangelog())
                .files(version.getFiles())
                .parsed(version.getParsed())
                .createdBy(UserResponse.fromEntity(version.getCreatedBy()))
                .createdAt(version.getCreatedAt())
                .build();
    }
}
