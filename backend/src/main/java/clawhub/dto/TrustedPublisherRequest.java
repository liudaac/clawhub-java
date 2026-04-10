package clawhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrustedPublisherRequest {

    @NotBlank(message = "Repository is required")
    private String repository;

    @NotBlank(message = "Repository ID is required")
    private String repositoryId;

    @NotBlank(message = "Repository owner is required")
    private String repositoryOwner;

    @NotBlank(message = "Repository owner ID is required")
    private String repositoryOwnerId;

    @NotBlank(message = "Workflow filename is required")
    private String workflowFilename;

    @NotBlank(message = "Environment is required")
    private String environment = "production";
}
