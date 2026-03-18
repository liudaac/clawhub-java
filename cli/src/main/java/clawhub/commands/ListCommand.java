package clawhub.commands;

import clawhub.api.ClawhubApi;
import clawhub.config.CliConfig;
import clawhub.model.Skill;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "list",
    description = "List installed skills"
)
public class ListCommand implements Callable<Integer> {
    
    @Option(names = {"-d", "--dir"}, description = "Skills directory", defaultValue = ".clawhub/skills")
    private String skillsDir;
    
    @Option(names = {"--server"}, description = "ClawHub server URL", defaultValue = "http://localhost:8080")
    private String serverUrl;
    
    @Override
    public Integer call() throws Exception {
        java.nio.file.Path dir = java.nio.file.Paths.get(skillsDir);
        
        if (!java.nio.file.Files.exists(dir)) {
            System.out.println("No skills installed.");
            System.out.println("Run 'clawhub install <skill-slug>' to install a skill.");
            return 0;
        }
        
        System.out.println("Installed skills:");
        System.out.println();
        
        java.nio.file.Files.walk(dir, 1)
            .filter(java.nio.file.Files::isDirectory)
            .filter(p -> !p.equals(dir))
            .forEach(p -> {
                String skillName = dir.relativize(p).toString();
                System.out.println("  " + skillName);
            });
        
        return 0;
    }
}
