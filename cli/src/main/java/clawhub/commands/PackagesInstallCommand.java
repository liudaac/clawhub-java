package clawhub.commands;

import clawhub.config.CliConfig;
import clawhub.model.Package;
import clawhub.model.PackageRelease;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "install",
    description = "Install a package",
    mixinStandardHelpOptions = true
)
public class PackagesInstallCommand implements Callable<Integer> {
    
    @Parameters(paramLabel = "<name>", description = "Package name")
    private String name;
    
    @Option(names = {"-v", "--version"}, description = "Specific version to install (default: latest)")
    private String version;
    
    @Option(names = {"-d", "--directory"}, description = "Installation directory (default: ./packages)")
    private String directory = "./packages";
    
    @Option(names = {"--force"}, description = "Force reinstall if already exists")
    private boolean force;
    
    @Option(names = {"--dry-run"}, description = "Show what would be installed")
    private boolean dryRun;
    
    private final CliConfig config = CliConfig.load();
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public Integer call() throws Exception {
        try {
            // Get package info
            System.out.println("Fetching package info...");
            Package pkg = getPackage(name);
            if (pkg == null) {
                System.err.println("Error: Package not found: " + name);
                return 1;
            }
            
            // Get release
            PackageRelease release;
            if (version != null) {
                System.out.println("Fetching version " + version + "...");
                release = getRelease(name, version);
            } else {
                System.out.println("Fetching latest version...");
                release = getLatestRelease(name);
            }
            
            if (release == null) {
                System.err.println("Error: Version not found");
                return 1;
            }
            
            // Check security
            if (release.getVtAnalysis() != null && 
                release.getVtAnalysis().getMaliciousCount() != null &&
                release.getVtAnalysis().getMaliciousCount() > 0) {
                System.err.println("⚠️  WARNING: This version has been flagged as malicious by VirusTotal!");
                System.err.println("   Malicious detections: " + release.getVtAnalysis().getMaliciousCount());
                if (!force) {
                    System.err.println("   Use --force to install anyway.");
                    return 1;
                }
            }
            
            if (release.getLlmAnalysis() != null && 
                "malicious".equalsIgnoreCase(release.getLlmAnalysis().getVerdict())) {
                System.err.println("⚠️  WARNING: This version has been flagged as malicious by LLM analysis!");
                if (!force) {
                    System.err.println("   Use --force to install anyway.");
                    return 1;
                }
            }
            
            if (pkg.getExecutesCode() != null && pkg.getExecutesCode()) {
                System.out.println("⚠️  This package executes code.");
            }
            
            Path installDir = Path.of(directory).toAbsolutePath().normalize()
                .resolve(pkg.getName());
            
            if (Files.exists(installDir)) {
                if (!force) {
                    System.err.println("Error: Package already installed at " + installDir);
                    System.err.println("Use --force to reinstall.");
                    return 1;
                }
                System.out.println("Removing existing installation...");
                deleteDirectory(installDir);
            }
            
            if (dryRun) {
                System.out.println();
                System.out.println("Dry run - would install:");
                System.out.println("  Package: " + pkg.getDisplayName());
                System.out.println("  Version: " + release.getVersion());
                System.out.println("  To: " + installDir);
                if (release.getFiles() != null) {
                    System.out.println("  Files: " + release.getFiles().size());
                }
                return 0;
            }
            
            // Create directory
            Files.createDirectories(installDir);
            
            // Download and install files
            System.out.println();
            System.out.println("Installing " + pkg.getDisplayName() + " v" + release.getVersion());
            System.out.println("To: " + installDir);
            System.out.println();
            
            if (release.getFiles() != null) {
                for (PackageRelease.FileInfo file : release.getFiles()) {
                    System.out.println("  Downloading: " + file.getPath());
                    // In production, this would download from storage service
                    // For now, create placeholder files
                    Path filePath = installDir.resolve(file.getPath());
                    Files.createDirectories(filePath.getParent());
                    Files.writeString(filePath, "# Placeholder for " + file.getPath() + "\n");
                }
            }
            
            // Save metadata
            Path metaPath = installDir.resolve(".clawhub-package.json");
            ObjectNode meta = mapper.createObjectNode()
                .put("name", pkg.getName())
                .put("version", release.getVersion())
                .put("installedAt", java.time.Instant.now().toString())
                .put("source", config.getServerUrl());
            Files.writeString(metaPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta));
            
            // Update install stats
            updateInstallStats(name);
            
            System.out.println();
            System.out.println("✓ Successfully installed " + pkg.getDisplayName() + " v" + release.getVersion());
            
            return 0;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    private Package getPackage(String name) throws IOException {
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/v1/packages/" + name)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    return null;
                }
                throw new IOException("Failed to get package: " + response.code());
            }
            
            JsonNode root = mapper.readTree(response.body().string());
            return mapper.treeToValue(root, Package.class);
        }
    }
    
    private PackageRelease getRelease(String name, String version) throws IOException {
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/v1/packages/" + name + "/versions/" + version)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            
            JsonNode root = mapper.readTree(response.body().string());
            return mapper.treeToValue(root, PackageRelease.class);
        }
    }
    
    private PackageRelease getLatestRelease(String name) throws IOException {
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/v1/packages/" + name + "/versions/latest")
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            
            JsonNode root = mapper.readTree(response.body().string());
            return mapper.treeToValue(root, PackageRelease.class);
        }
    }
    
    private void updateInstallStats(String name) {
        // Fire and forget - don't block on this
        try {
            Request request = new Request.Builder()
                .url(config.getServerUrl() + "/api/v1/packages/" + name + "/install")
                .post(okhttp3.RequestBody.create("", okhttp3.MediaType.parse("application/json")))
                .build();
            client.newCall(request).execute().close();
        } catch (IOException e) {
            // Ignore stats update errors
        }
    }
    
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                stream.forEach(child -> {
                    try {
                        deleteDirectory(child);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.delete(dir);
    }
}
