package clawhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Data
public class CliConfig {
    
    private static final String CONFIG_DIR = ".config/clawhub";
    private static final String CONFIG_FILE = "config.json";
    
    private String serverUrl = "http://localhost:8080";
    private String token;
    
    public static CliConfig load() throws Exception {
        Path configPath = getConfigPath();
        
        if (!Files.exists(configPath)) {
            return new CliConfig();
        }
        
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(configPath.toFile(), CliConfig.class);
    }
    
    public void save() throws Exception {
        Path configPath = getConfigPath();
        Files.createDirectories(configPath.getParent());
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), this);
    }
    
    private static Path getConfigPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, CONFIG_DIR, CONFIG_FILE);
    }
}
