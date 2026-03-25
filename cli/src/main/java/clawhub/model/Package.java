package clawhub.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Package {
    
    private UUID id;
    private String name;
    private String displayName;
    private String family;
    private String runtimeId;
    private String channel;
    private Boolean isOfficial;
    private String summary;
    private UUID ownerUserId;
    private String ownerHandle;
    private UUID ownerPublisherId;
    private String ownerPublisherHandle;
    private Map<String, Object> compatibility;
    private Map<String, Object> capabilities;
    private Map<String, Object> verification;
    private String scanStatus;
    private Long statsDownloads;
    private Long statsInstalls;
    private Integer statsStars;
    private Integer statsVersions;
    private String latestVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean executesCode;
    private String verificationTier;
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }
    
    public String getRuntimeId() { return runtimeId; }
    public void setRuntimeId(String runtimeId) { this.runtimeId = runtimeId; }
    
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    
    @JsonProperty("isOfficial")
    public Boolean getIsOfficial() { return isOfficial; }
    public void setIsOfficial(Boolean isOfficial) { this.isOfficial = isOfficial; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    
    public String getOwnerHandle() { return ownerHandle; }
    public void setOwnerHandle(String ownerHandle) { this.ownerHandle = ownerHandle; }
    
    public UUID getOwnerPublisherId() { return ownerPublisherId; }
    public void setOwnerPublisherId(UUID ownerPublisherId) { this.ownerPublisherId = ownerPublisherId; }
    
    public String getOwnerPublisherHandle() { return ownerPublisherHandle; }
    public void setOwnerPublisherHandle(String ownerPublisherHandle) { this.ownerPublisherHandle = ownerPublisherHandle; }
    
    public Map<String, Object> getCompatibility() { return compatibility; }
    public void setCompatibility(Map<String, Object> compatibility) { this.compatibility = compatibility; }
    
    public Map<String, Object> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Object> capabilities) { this.capabilities = capabilities; }
    
    public Map<String, Object> getVerification() { return verification; }
    public void setVerification(Map<String, Object> verification) { this.verification = verification; }
    
    public String getScanStatus() { return scanStatus; }
    public void setScanStatus(String scanStatus) { this.scanStatus = scanStatus; }
    
    public Long getStatsDownloads() { return statsDownloads; }
    public void setStatsDownloads(Long statsDownloads) { this.statsDownloads = statsDownloads; }
    
    public Long getStatsInstalls() { return statsInstalls; }
    public void setStatsInstalls(Long statsInstalls) { this.statsInstalls = statsInstalls; }
    
    public Integer getStatsStars() { return statsStars; }
    public void setStatsStars(Integer statsStars) { this.statsStars = statsStars; }
    
    public Integer getStatsVersions() { return statsVersions; }
    public void setStatsVersions(Integer statsVersions) { this.statsVersions = statsVersions; }
    
    public String getLatestVersion() { return latestVersion; }
    public void setLatestVersion(String latestVersion) { this.latestVersion = latestVersion; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Boolean getExecutesCode() { return executesCode; }
    public void setExecutesCode(Boolean executesCode) { this.executesCode = executesCode; }
    
    public String getVerificationTier() { return verificationTier; }
    public void setVerificationTier(String verificationTier) { this.verificationTier = verificationTier; }
    
    public boolean isSkill() {
        return "skill".equalsIgnoreCase(family);
    }
    
    public boolean isCodePlugin() {
        return "code_plugin".equalsIgnoreCase(family) || "code-plugin".equalsIgnoreCase(family);
    }
    
    public boolean isBundlePlugin() {
        return "bundle_plugin".equalsIgnoreCase(family) || "bundle-plugin".equalsIgnoreCase(family);
    }
    
    public boolean isOfficial() {
        return Boolean.TRUE.equals(isOfficial) || "official".equalsIgnoreCase(channel);
    }
}
