package clawhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageReleaseListResponse {

    private List<PackageReleaseResponse> releases;
    private long total;
    private int page;
    private int size;
    private boolean hasMore;
}
