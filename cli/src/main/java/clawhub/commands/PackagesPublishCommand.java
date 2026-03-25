package clawhub.commands;

import clawhub.config.CliConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
    name = "publish",
    description = "Publish a new package or version",
    mixinStandardHelpOptions = true
)
public class PackagesPublishCommand implements Callable<Integer> {
    
    @Parameters(paramLabel = "<path>", description = "Path to package directory")
    private String packagePath;
    
    @Option(names = {"-n", "--name"}, description = "Package name (default: directory name)")
    private String name;
    
    @Option(names = {"-d", "--display-name"}, description = "Display name")
    private String displayName;
    
    @Option(names = {"-s", "--summary"}, description = "Package summary/description")
    private String summary;
    
    @Option(names = {"-f", "--family"}, description = "Package family: skill, code-plugin, bundle-plugin", required = true)
    private String family;
    
    @Option(names = {"-c", "--channel"}, description = "Channel: official, community, private", defaultValue = "community")
    private String channel;
    
    @Option(names = {"-v", "--version"}, description = "Version to publish", required = true)
    private String version;
    
    @Option(names = {"--changelog"}, description = "Changelog for this version")
    private String changelog;
    
    @Option(names = {"--publisher"}, description = "Publisher ID to publish under")
    private String publisherId;
    
    @Option(names = {"--runtime"}, description = "Runtime ID")
    private String runtimeId;
    
    @Option(names = {"--dry-run"}, description = "Show what would be published without actually publishing")
    private boolean dryRun;
    
    private final CliConfig config = CliConfig.load();
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public Integer call() throws Exception {
        // Check authentication
        if (config.getToken() == null || config.getToken().isEmpty()) {
            System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
            return 1;
        }
        
        Path path = Path.of(packagePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            System.err.println("Error: Path does not exist: " + path);
            return 1;
        }
        if (!Files.isDirectory(path)) {
            System.err.println("Error: Path is not a directory: " + path);
            return 1;
        }
        
        // Derive name from directory if not provided
        if (name == null) {
            name = path.getFileName().toString().toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-");
        }
        
        if (displayName == null) {
            displayName = path.getFileName().toString();
        }
        
        // Check if package exists
        boolean exists = checkPackageExists(name);
        
        if (dryRun) {
            System.out.println("Dry run - would publish:");
            System.out.println("  Package: " + name);
            System.out.println("  Display Name: " + displayName);
            System.out.println("  Family: " + family);
            System.out.println("  Channel: " + channel);
            System.out.println("  Version: " + version);
            System.out.println("  Path: " + path);
            System.out.println("  New package: " + !exists);
            return 0;
        }
        
        try {
            // Create package if it doesn't exist
            if (!exists) {
                System.out.println("Creating new package: " + name);
                createPackage();
            }
            
            // Collect files
            System.out.println("Collecting files...");
            List<FileEntry> files = collectFiles(path);
            
            if (files.isEmpty()) {
                System.err.println("Error: No files found in " + path);
                return 1;
            }
            
            System.out.println("Found " + files.size() + " files");
            
            // Upload files and build file list
            ArrayNode filesArray = mapper.createArrayNode();
            for (FileEntry entry : files) {
                System.out.println("  Uploading: " + entry.relativePath + " (" + formatSize(entry.size) + ")");
                String storageId = uploadFile(entry);
                
                ObjectNode fileNode = mapper.createObjectNode()
                    .put("path", entry.relativePath)
                    .put("size", entry.size)
                    .put("sha256", entry.sha256)
                    .put("storageId", storageId);
                filesArray.add(fileNode);
            }
            
            // Create release
            System.out.println("Creating release " + version + "...");
            createRelease(filesArray);
            
            System.out.println();
            System.out.println("✓ Successfully published " + name + " v" + version);
            System.out.println("  View at: " + config.getServerUrl() + "/packages/" + name);
            
            return 0;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    private boolean checkPackageExists(String name) throws IOException {
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/v1/packages/" + name)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }
    
    private void createPackage() throws IOException {
        ObjectNode body = mapper.createObjectNode()
            .put("name", name)
            .put("displayName", displayName)
            .put("summary", summary != null ? summary : "")
            .put("family", family.toUpperCase().replace("-", "_"))
            .put("channel", channel.toUpperCase());
        
        if (publisherId != null) {
            body.put("ownerPublisherId", publisherId);
        }
        if (runtimeId != null) {
            body.put("runtimeId", runtimeId);
        }
        
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/v1/packages")
            .header("Authorization", "Bearer " + config.getToken())
            .post(RequestBody.create(mapper.writeValueAsString(body), 
                MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String error = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Failed to create package: " + response.code() + " - " + error);
            }
        }
    }
    
    private List<FileEntry> collectFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> !shouldIgnore(p))
                .map(p -> {
                    try {
                        FileEntry entry = new FileEntry();
                        entry.path = p;
                        entry.relativePath = root.relativize(p).toString().replace("\\", "/");
                        entry.size = Files.size(p);
                        entry.content = Files.readAllBytes(p);
                        entry.sha256 = sha256(entry.content);
                        return entry;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        }
    }
    
    private boolean shouldIgnore(Path path) {
        String name = path.getFileName().toString();
        // Ignore common files
        if (name.startsWith(".")) return true;
        if (name.equals("node_modules") || name.equals("target") || name.equals("build")) return true;
        if (name.endsWith(".log") || name.endsWith(".tmp")) return true;
        return false;
    }
    
    private String sha256(byte[] data) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Failed to compute SHA-256", e);
        }
    }
    
    private String uploadFile(FileEntry entry) throws IOException {
        // For now, return a placeholder - in production this would upload to storage
        // This is a simplified implementation
        return "storage-" + entry.sha256.substring(0, 16);
    }
    
    private void createRelease(ArrayNode filesArray) throws IOException {
        ObjectNode body = mapper.createObjectNode()
            .put("version", version)
            .put("changelog", changelog != null ? changelog : "")
            .set("files", filesArray);
        
        // Compute integrity hash of all files
        StringBuilder integrityBuilder = new StringBuilder();
        for (JsonNode file : filesArray) {
            integrityBuilder.append(file.get("sha256").asText());
        }
        String integrity = sha256(integrityBuilder.toString().getBytes());
        body.put("integritySha256", integrity);
        
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/v1/packages/" + name + "/versions")
            .header("Authorization", "Bearer " + config.getToken())
            .post(RequestBody.create(mapper.writeValueAsString(body), 
                MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String error = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Failed to create release: " + response.code() + " - " + error);
            }
        }
    }
    
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
    
    private static class FileEntry {
        Path path;
        String relativePath;
        long size;
        byte[] content;
        String sha256;
    }
}