package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageReleaseRequest {

    @NotBlank(message = "Version is required")
    private String version;

    private String changelog;

    @NotNull(message = "Files are required")
    private List<FileInfo> files;

    private String integritySha256;

    private Map<String, Object> compatibility;

    private Map<String, Object> capabilities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String path;
        private Long size;
        private String sha256;
        private String contentType;
    }
}
