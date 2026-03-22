package clawhub.commands;

import clawhub.config.CliConfig;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "uninstall",
    description = "Uninstall a skill",
    aliases = {"remove", "rm"}
)
public class UninstallCommand implements Callable<Integer> {

    @CommandLine.Parameters(paramLabel = "<skill>", description = "Skill slug to uninstall")
    private String skillSlug;

    @CommandLine.Option(names = {"-f", "--force"}, description = "Force uninstall without confirmation")
    private boolean force;

    @CommandLine.Option(names = {"--keep-data"}, description = "Keep skill data after uninstall")
    private boolean keepData;

    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        Path skillsDir = config.getSkillsDirectory();
        Path skillDir = skillsDir.resolve(skillSlug);

        if (!Files.exists(skillDir)) {
            System.err.println("Error: Skill '" + skillSlug + "' is not installed.");
            return 1;
        }

        // Confirm uninstall
        if (!force) {
            System.out.print("Are you sure you want to uninstall '" + skillSlug + "'? [y/N] ");
            String response = System.console().readLine();
            if (!response.equalsIgnoreCase("y") && !response.equalsIgnoreCase("yes")) {
                System.out.println("Uninstall cancelled.");
                return 0;
            }
        }

        try {
            // Backup before uninstall if not keeping data
            if (!keepData) {
                Path backupDir = skillsDir.resolve(".backups").resolve(skillSlug + "-" + System.currentTimeMillis());
                Files.createDirectories(backupDir.getParent());
                moveDirectory(skillDir, backupDir);
                System.out.println("✓ Skill '" + skillSlug + "' uninstalled (backed up to " + backupDir.getFileName() + ")");
            } else {
                deleteDirectory(skillDir);
                System.out.println("✓ Skill '" + skillSlug + "' uninstalled");
            }

            return 0;
        } catch (IOException e) {
            System.err.println("Error uninstalling skill: " + e.getMessage());
            return 1;
        }
    }

    private void moveDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }
        Files.move(source, target);
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walk(dir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
    }
}
