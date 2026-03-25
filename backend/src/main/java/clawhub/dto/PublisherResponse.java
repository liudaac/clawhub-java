package clawhub.dto;

import clawhub.entity.Publisher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherResponse {

    private UUID id;
    private String kind;
    private String handle;
    private String displayName;
    private String bio;
    private String image;
    private UUID linkedUserId;
    private Boolean trustedPublisher;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    // Stats
    private Long statsDownloads;
    private Integer statsStars;
    private Integer statsVersions;

    public static PublisherResponse fromEntity(Publisher publisher) {
        if (publisher == null) {
            return null;
        }
        return PublisherResponse.builder()
                .id(publisher.getId())
                .kind(publisher.getKind() != null ? publisher.getKind().name().toLowerCase() : null)
                .handle(publisher.getHandle())
                .displayName(publisher.getDisplayName())
                .bio(publisher.getBio())
                .image(publisher.getImage())
                .linkedUserId(publisher.getLinkedUserId())
                .trustedPublisher(publisher.getTrustedPublisher())
                .isActive(publisher.isActive())
                .createdAt(publisher.getCreatedAt())
                .updatedAt(publisher.getUpdatedAt())
                .build();
    }

    public static PublisherResponse fromEntityWithStats(Publisher publisher, Long downloads, Integer stars, Integer versions) {
        PublisherResponse response = fromEntity(publisher);
        if (response != null) {
            response.setStatsDownloads(downloads);
            response.setStatsStars(stars);
            response.setStatsVersions(versions);
        }
        return response;
    }
}
