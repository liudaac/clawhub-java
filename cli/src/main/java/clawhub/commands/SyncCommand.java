package clawhub.commands;

import clawhub.api.ClawhubApi;
import clawhub.config.CliConfig;
import clawhub.model.Skill;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "sync",
    description = "Sync installed skills with registry"
)
public class SyncCommand implements Callable<Integer> {
    
    @Option(names = {"-d", "--dir"}, description = "Skills directory", defaultValue = ".clawhub/skills")
    private String skillsDir;
    
    @Option(names = {"--server"}, description = "ClawHub server URL", defaultValue = "http://localhost:8080")
    private String serverUrl;
    
    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        config.setServerUrl(serverUrl);
        
        ClawhubApi api = new ClawhubApi(config);
        
        Path dir = Paths.get(skillsDir);
        
        if (!Files.exists(dir)) {
            System.out.println("No skills directory found.");
            return 0;
        }
        
        System.out.println("Checking for updates...");
        System.out.println();
        
        // List installed skills
        Files.walk(dir, 1)
            .filter(Files::isDirectory)
            .filter(p -> !p.equals(dir))
            .forEach(p -> {
                String skillSlug = dir.relativize(p).toString();
                try {
                    Skill skill = api.getSkill(skillSlug);
                    if (skill != null && skill.getLatestVersion() != null) {
                        System.out.println(skillSlug + ":");
                        System.out.println("  Installed: ?");
                        System.out.println("  Latest: " + skill.getLatestVersion().getVersion());
                        System.out.println();
                    }
                } catch (Exception e) {
                    System.err.println("Error checking " + skillSlug + ": " + e.getMessage());
                }
            });
        
        return 0;
    }
}
