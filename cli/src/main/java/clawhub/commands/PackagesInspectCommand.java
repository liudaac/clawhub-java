package clawhub.commands;

import clawhub.config.CliConfig;
import clawhub.model.Package;
import clawhub.model.PackageRelease;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(
    name = "inspect",
    description = "View detailed information about a package",
    mixinStandardHelpOptions = true
)
public class PackagesInspectCommand implements Callable<Integer> {
    
    @Parameters(paramLabel = "<name>", description = "Package name")
    private String name;
    
    @Option(names = {"-v", "--version"}, description = "Specific version to inspect")
    private String version;
    
    @Option(names = {"--json"}, description = "Output as JSON")
    private boolean json;
    
    private final CliConfig config = CliConfig.load();
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public Integer call() throws Exception {
        try {
            // Get package info
            Request packageRequest = new Request.Builder()
                .url(config.getServerUrl() + "/api/v1/packages/" + name)
                .build();
            
            Package pkg;
            try (Response response = client.newCall(packageRequest).execute()) {
                if (!response.isSuccessful()) {
                    if (response.code() == 404) {
                        System.err.println("Package not found: " + name);
                        return 1;
                    }
                    System.err.println("Error: " + response.code() + " - " + response.message());
                    return 1;
                }
                
                JsonNode root = mapper.readTree(response.body().string());
                pkg = mapper.treeToValue(root, Package.class);
            }
            
            // Get specific version or latest
            PackageRelease release = null;
            if (version != null) {
                release = getRelease(name, version);
            }
            
            if (json) {
                printJson(pkg, release);
            } else {
                printHumanReadable(pkg, release);
            }
            
            return 0;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    private PackageRelease getRelease(String packageName, String version) throws IOException {
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/v1/packages/" + packageName + "/versions/" + version)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            
            JsonNode root = mapper.readTree(response.body().string());
            return mapper.treeToValue(root, PackageRelease.class);
        }
    }
    
    private void printHumanReadable(Package pkg, PackageRelease release) {
        System.out.println("Package: " + pkg.getDisplayName());
        System.out.println("Name: " + pkg.getName());
        System.out.println("Family: " + pkg.getFamily());
        System.out.println("Channel: " + pkg.getChannel());
        if (pkg.isOfficial()) {
            System.out.println("Official: ✓");
        }
        System.out.println();
        
        if (pkg.getSummary() != null) {
            System.out.println("Summary:");
            System.out.println("  " + pkg.getSummary());
            System.out.println();
        }
        
        System.out.println("Owner: " + pkg.getOwnerHandle());
        if (pkg.getOwnerPublisherHandle() != null) {
            System.out.println("Publisher: " + pkg.getOwnerPublisherHandle());
        }
        System.out.println();
        
        System.out.println("Statistics:");
        System.out.println("  Downloads: " + (pkg.getStatsDownloads() != null ? pkg.getStatsDownloads() : 0));
        System.out.println("  Installs: " + (pkg.getStatsInstalls() != null ? pkg.getStatsInstalls() : 0));
        System.out.println("  Stars: " + (pkg.getStatsStars() != null ? pkg.getStatsStars() : 0));
        System.out.println("  Versions: " + (pkg.getStatsVersions() != null ? pkg.getStatsVersions() : 0));
        System.out.println();
        
        if (pkg.getLatestVersion() != null) {
            System.out.println("Latest Version: " + pkg.getLatestVersion());
        }
        
        if (pkg.getExecutesCode() != null && pkg.getExecutesCode()) {
            System.out.println();
            System.out.println("⚠️  This package executes code.");
        }
        
        if (pkg.getVerificationTier() != null && !"none".equals(pkg.getVerificationTier())) {
            System.out.println("Verification Tier: " + pkg.getVerificationTier());
        }
        
        if (release != null) {
            System.out.println();
            System.out.println("Version Details: " + release.getVersion());
            if (release.getChangelog() != null) {
                System.out.println("Changelog:");
                System.out.println("  " + release.getChangelog().replace("\n", "\n  "));
            }
            
            if (release.getFiles() != null && !release.getFiles().isEmpty()) {
                System.out.println();
                System.out.println("Files:");
                for (PackageRelease.FileInfo file : release.getFiles()) {
                    System.out.println("  - " + file.getPath() + " (" + formatSize(file.getSize()) + ")");
                }
            }
            
            // Security info
            if (release.getVtAnalysis() != null) {
                System.out.println();
                System.out.println("VirusTotal Scan:");
                System.out.println("  Status: " + release.getVtAnalysis().getStatus());
                if (release.getVtAnalysis().getMaliciousCount() != null && release.getVtAnalysis().getMaliciousCount() > 0) {
                    System.out.println("  ⚠️  Malicious: " + release.getVtAnalysis().getMaliciousCount());
                }
            }
            
            if (release.getLlmAnalysis() != null) {
                System.out.println();
                System.out.println("LLM Security Analysis:");
                System.out.println("  Verdict: " + release.getLlmAnalysis().getVerdict());
                System.out.println("  Confidence: " + release.getLlmAnalysis().getConfidence());
            }
        }
    }
    
    private void printJson(Package pkg, PackageRelease release) throws IOException {
        if (release != null) {
            // Create combined output
            JsonNode pkgNode = mapper.valueToTree(pkg);
            JsonNode releaseNode = mapper.valueToTree(release);
            
            JsonNode combined = mapper.createObjectNode()
                .setAll((com.fasterxml.jackson.databind.node.ObjectNode) pkgNode)
                .setAll((com.fasterxml.jackson.databind.node.ObjectNode) releaseNode);
            
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(combined));
        } else {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pkg));
        }
    }
    
    private String formatSize(Long size) {
        if (size == null) return "unknown";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}