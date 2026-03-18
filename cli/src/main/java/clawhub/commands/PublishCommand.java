package clawhub.commands;

import clawhub.api.ClawhubApi;
import clawhub.config.CliConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.Callable;

@Command(
    name = "publish",
    description = "Publish a skill to ClawHub"
)
public class PublishCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Path to skill directory")
    private String skillPath;
    
    @Option(names = {"-m", "--message"}, description = "Version changelog message", required = true)
    private String changelog;
    
    @Option(names = {"-v", "--version"}, description = "Version (e.g., 1.0.0)", required = true)
    private String version;
    
    @Option(names = {"--server"}, description = "ClawHub server URL", defaultValue = "http://localhost:8080")
    private String serverUrl;
    
    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        
        if (config.getToken() == null) {
            System.err.println("Error: Not logged in. Run 'clawhub login' first.");
            return 1;
        }
        
        config.setServerUrl(serverUrl);
        ClawhubApi api = new ClawhubApi(config);
        
        Path path = Paths.get(skillPath);
        
        if (!Files.exists(path)) {
            System.err.println("Error: Directory not found: " + skillPath);
            return 1;
        }
        
        // Read SKILL.md or config file
        Path skillMd = path.resolve("SKILL.md");
        Path configJson = path.resolve("clawhub.json");
        
        String slug;
        String displayName;
        String summary = null;
        
        if (Files.exists(configJson)) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode configNode = mapper.readTree(configJson.toFile());
            slug = configNode.get("slug").asText();
            displayName = configNode.get("name").asText();
            if (configNode.has("summary")) {
                summary = configNode.get("summary").asText();
            }
        } else if (Files.exists(skillMd)) {
            // Parse SKILL.md
            String content = Files.readString(skillMd);
            // Simple parsing - in real implementation, parse frontmatter
            slug = path.getFileName().toString();
            displayName = slug;
        } else {
            System.err.println("Error: No SKILL.md or clawhub.json found in " + skillPath);
            return 1;
        }
        
        System.out.println("Publishing skill: " + slug);
        System.out.println("  Version: " + version);
        System.out.println();
        
        try {
            // Create skill if not exists
            try {
                api.getSkill(slug);
            } catch (Exception e) {
                // Skill doesn't exist, create it
                System.out.println("Creating new skill...");
                api.createSkill(slug, displayName, summary);
            }
            
            // Collect files
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode filesNode = mapper.createArrayNode();
            
            Files.walk(path)
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    try {
                        String relativePath = path.relativize(p).toString();
                        byte[] content = Files.readAllBytes(p);
                        
                        ObjectNode fileNode = mapper.createObjectNode();
                        fileNode.put("path", relativePath);
                        fileNode.put("size", content.length);
                        fileNode.put("sha256", sha256(content));
                        
                        filesNode.add(fileNode);
                        
                        // Upload file
                        System.out.println("Uploading: " + relativePath);
                        String storageId = api.uploadFile(content, relativePath);
                        fileNode.put("storageId", storageId);
                    } catch (Exception ex) {
                        System.err.println("Error uploading " + p + ": " + ex.getMessage());
                    }
                });
            
            // Create version
            System.out.println("Creating version...");
            api.createVersion(slug, version, changelog, filesNode);
            
            System.out.println();
            System.out.println("Successfully published!");
            System.out.println("View at: " + serverUrl + "/skills/" + slug);
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    private String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
