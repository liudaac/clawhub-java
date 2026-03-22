package clawhub.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileContentResponse {
    private String path;
    private String content;
    private String contentType;
    private long size;
    private String sha256;
    private boolean isText;
    private boolean isImage;
    private boolean isBinary;
}
