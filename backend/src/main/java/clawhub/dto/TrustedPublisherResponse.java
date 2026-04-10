package clawhub.dto;

import clawhub.entity.SkillTrustedPublisher;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TrustedPublisherResponse {

    private Long id;
    private String repository;
    private String repositoryOwner;
    private String workflowFilename;
    private String environment;
    private Instant createdAt;

    public static TrustedPublisherResponse fromEntity(SkillTrustedPublisher publisher) {
        if (publisher == null) {
            return null;
        }
        return TrustedPublisherResponse.builder()
                .id(publisher.getId())
                .repository(publisher.getRepository())
                .repositoryOwner(publisher.getRepositoryOwner())
                .workflowFilename(publisher.getWorkflowFilename())
                .environment(publisher.getEnvironment())
                .createdAt(publisher.getCreatedAt())
                .build();
    }
}
