package clawhub.commands;

import clawhub.api.ClawhubApi;
import clawhub.config.CliConfig;
import clawhub.model.Skill;
import clawhub.model.SkillVersion;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Command(
    name = "install",
    description = "Install a skill"
)
public class InstallCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Skill slug (e.g., 'owner/skill-name')")
    private String skillSlug;
    
    @Option(names = {"-v", "--version"}, description = "Version to install (default: latest)")
    private String version;
    
    @Option(names = {"-d", "--dir"}, description = "Installation directory", defaultValue = ".clawhub/skills")
    private String installDir;
    
    @Option(names = {"--server"}, description = "ClawHub server URL", defaultValue = "http://localhost:8080")
    private String serverUrl;
    
    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        config.setServerUrl(serverUrl);
        
        ClawhubApi api = new ClawhubApi(config);
        
        try {
            System.out.println("Installing skill: " + skillSlug);
            
            // Get skill info
            Skill skill = api.getSkill(skillSlug);
            
            if (skill == null) {
                System.err.println("Error: Skill not found: " + skillSlug);
                return 1;
            }
            
            SkillVersion skillVersion = version != null 
                ? api.getSkillVersion(skillSlug, version)
                : skill.getLatestVersion();
            
            if (skillVersion == null) {
                System.err.println("Error: Version not found: " + (version != null ? version : "latest"));
                return 1;
            }
            
            System.out.println("  Name: " + skill.getDisplayName());
            System.out.println("  Version: " + skillVersion.getVersion());
            System.out.println();
            
            // Create install directory
            Path targetDir = Paths.get(installDir, skillSlug);
            Files.createDirectories(targetDir);
            
            // Download files
            if (skillVersion.getFiles() != null) {
                for (var file : skillVersion.getFiles()) {
                    System.out.println("Downloading: " + file.getPath());
                    
                    byte[] content = api.downloadFile(file.getStorageId());
                    Path filePath = targetDir.resolve(file.getPath());
                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, content);
                }
            }
            
            System.out.println();
            System.out.println("Successfully installed to: " + targetDir.toAbsolutePath());
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
