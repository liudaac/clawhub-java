package clawhub.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PackageRelease {
    
    private UUID id;
    private UUID packageId;
    private String packageName;
    private String version;
    private String changelog;
    private String changelogSource;
    private List<FileInfo> files;
    private String integritySha256;
    private Map<String, Object> compatibility;
    private Map<String, Object> capabilities;
    private Map<String, Object> verification;
    private VirusTotalAnalysis vtAnalysis;
    private LlmSecurityAnalysis llmAnalysis;
    private StaticScanResult staticScan;
    private UUID publishedBy;
    private String publishedByHandle;
    private Instant createdAt;
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getPackageId() { return packageId; }
    public void setPackageId(UUID packageId) { this.packageId = packageId; }
    
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getChangelog() { return changelog; }
    public void setChangelog(String changelog) { this.changelog = changelog; }
    
    public String getChangelogSource() { return changelogSource; }
    public void setChangelogSource(String changelogSource) { this.changelogSource = changelogSource; }
    
    public List<FileInfo> getFiles() { return files; }
    public void setFiles(List<FileInfo> files) { this.files = files; }
    
    public String getIntegritySha256() { return integritySha256; }
    public void setIntegritySha256(String integritySha256) { this.integritySha256 = integritySha256; }
    
    public Map<String, Object> getCompatibility() { return compatibility; }
    public void setCompatibility(Map<String, Object> compatibility) { this.compatibility = compatibility; }
    
    public Map<String, Object> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Object> capabilities) { this.capabilities = capabilities; }
    
    public Map<String, Object> getVerification() { return verification; }
    public void setVerification(Map<String, Object> verification) { this.verification = verification; }
    
    public VirusTotalAnalysis getVtAnalysis() { return vtAnalysis; }
    public void setVtAnalysis(VirusTotalAnalysis vtAnalysis) { this.vtAnalysis = vtAnalysis; }
    
    public LlmSecurityAnalysis getLlmAnalysis() { return llmAnalysis; }
    public void setLlmAnalysis(LlmSecurityAnalysis llmAnalysis) { this.llmAnalysis = llmAnalysis; }
    
    public StaticScanResult getStaticScan() { return staticScan; }
    public void setStaticScan(StaticScanResult staticScan) { this.staticScan = staticScan; }
    
    public UUID getPublishedBy() { return publishedBy; }
    public void setPublishedBy(UUID publishedBy) { this.publishedBy = publishedBy; }
    
    public String getPublishedByHandle() { return publishedByHandle; }
    public void setPublishedByHandle(String publishedByHandle) { this.publishedByHandle = publishedByHandle; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileInfo {
        private String path;
        private Long size;
        private String sha256;
        private String contentType;
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        
        public String getSha256() { return sha256; }
        public void setSha256(String sha256) { this.sha256 = sha256; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VirusTotalAnalysis {
        private String scanId;
        private String status;
        private Integer maliciousCount;
        private Integer suspiciousCount;
        private Integer harmlessCount;
        private Integer undetectedCount;
        private String permalink;
        private Instant scannedAt;
        
        public String getScanId() { return scanId; }
        public void setScanId(String scanId) { this.scanId = scanId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getMaliciousCount() { return maliciousCount; }
        public void setMaliciousCount(Integer maliciousCount) { this.maliciousCount = maliciousCount; }
        
        public Integer getSuspiciousCount() { return suspiciousCount; }
        public void setSuspiciousCount(Integer suspiciousCount) { this.suspiciousCount = suspiciousCount; }
        
        public Integer getHarmlessCount() { return harmlessCount; }
        public void setHarmlessCount(Integer harmlessCount) { this.harmlessCount = harmlessCount; }
        
        public Integer getUndetectedCount() { return undetectedCount; }
        public void setUndetectedCount(Integer undetectedCount) { this.undetectedCount = undetectedCount; }
        
        public String getPermalink() { return permalink; }
        public void setPermalink(String permalink) { this.permalink = permalink; }
        
        public Instant getScannedAt() { return scannedAt; }
        public void setScannedAt(Instant scannedAt) { this.scannedAt = scannedAt; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LlmSecurityAnalysis {
        private String verdict;
        private String confidence;
        private String summary;
        private String guidance;
        private Instant analyzedAt;
        private String model;
        
        public String getVerdict() { return verdict; }
        public void setVerdict(String verdict) { this.verdict = verdict; }
        
        public String getConfidence() { return confidence; }
        public void setConfidence(String confidence) { this.confidence = confidence; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public String getGuidance() { return guidance; }
        public void setGuidance(String guidance) { this.guidance = guidance; }
        
        public Instant getAnalyzedAt() { return analyzedAt; }
        public void setAnalyzedAt(Instant analyzedAt) { this.analyzedAt = analyzedAt; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StaticScanResult {
        private String status;
        private Integer findingCount;
        private Instant scannedAt;
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getFindingCount() { return findingCount; }
        public void setFindingCount(Integer findingCount) { this.findingCount = findingCount; }
        
        public Instant getScannedAt() { return scannedAt; }
        public void setScannedAt(Instant scannedAt) { this.scannedAt = scannedAt; }
    }
}
