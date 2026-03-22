package clawhub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class SkillSearchRequest {
    private String query;
    private String sortBy = "created";
    private String sortDirection = "desc";
    
    @Min(0)
    private int page = 0;
    
    @Min(1)
    @Max(100)
    private int size = 20;
    
    private String cursor;
    private boolean highlightedOnly = false;
    private boolean nonSuspiciousOnly = false;
    private List<String> tags;
    private String owner;
}
