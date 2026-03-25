package clawhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageSearchRequest {

    private String query;
    private String family;
    private String channel;
    private Boolean official;
    private String runtimeId;
    private Boolean executesCode;
    private String verificationTier;
    private int page;
    private int size;
}
