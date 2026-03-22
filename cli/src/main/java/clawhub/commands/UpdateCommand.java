package clawhub.commands;

import clawhub.api.ClawhubApiClient;
import clawhub.config.CliConfig;
import clawhub.model.SkillInfo;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "update",
    description = "Update installed skills to the latest version"
)
public class UpdateCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"-s", "--skill"}, description = "Specific skill to update")
    private String skillSlug;

    @CommandLine.Option(names = {"-f", "--force"}, description = "Force update even if already at latest version")
    private boolean force;

    @CommandLine.Option(names = {"--dry-run"}, description = "Show what would be updated without making changes")
    private boolean dryRun;

    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        if (!config.isAuthenticated()) {
            System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
            return 1;
        }

        ClawhubApiClient apiClient = new ClawhubApiClient(config);
        Path skillsDir = config.getSkillsDirectory();

        if (skillSlug != null) {
            // Update specific skill
            return updateSkill(apiClient, skillsDir, skillSlug);
        } else {
            // Update all skills
            return updateAllSkills(apiClient, skillsDir);
        }
    }

    private int updateSkill(ClawhubApiClient apiClient, Path skillsDir, String slug) {
        try {
            Path skillDir = skillsDir.resolve(slug);
            if (!Files.exists(skillDir)) {
                System.err.println("Error: Skill '" + slug + "' is not installed.");
                return 1;
            }

            System.out.println("Checking for updates: " + slug);
            
            SkillInfo localInfo = readLocalSkillInfo(skillDir);
            SkillInfo remoteInfo = apiClient.getSkillInfo(slug);

            if (localInfo == null) {
                System.err.println("Error: Could not read local skill info for '" + slug + "'");
                return 1;
            }

            if (remoteInfo == null) {
                System.err.println("Error: Could not fetch remote info for '" + slug + "'");
                return 1;
            }

            String localVersion = localInfo.getVersion();
            String remoteVersion = remoteInfo.getLatestVersion();

            if (localVersion.equals(remoteVersion) && !force) {
                System.out.println("Skill '" + slug + "' is already at the latest version (" + localVersion + ")");
                return 0;
            }

            System.out.println("Updating " + slug + " from " + localVersion + " to " + remoteVersion);

            if (dryRun) {
                System.out.println("[DRY RUN] Would update " + slug + " to version " + remoteVersion);
                return 0;
            }

            // Download and install new version
            Path tempDir = Files.createTempDirectory("clawhub-update-" + slug);
            try {
                apiClient.downloadSkill(slug, remoteVersion, tempDir);
                
                // Backup current version
                Path backupDir = skillsDir.resolve(".backups").resolve(slug + "-" + localVersion);
                if (!Files.exists(backupDir.getParent())) {
                    Files.createDirectories(backupDir.getParent());
                }
                moveDirectory(skillDir, backupDir);
                
                // Install new version
                moveDirectory(tempDir.resolve(slug), skillDir);
                
                System.out.println("✓ Successfully updated " + slug + " to version " + remoteVersion);
                return 0;
            } finally {
                // Cleanup temp directory
                deleteDirectory(tempDir);
            }
        } catch (Exception e) {
            System.err.println("Error updating skill '" + slug + "': " + e.getMessage());
            return 1;
        }
    }

    private int updateAllSkills(ClawhubApiClient apiClient, Path skillsDir) {
        try {
            if (!Files.exists(skillsDir)) {
                System.out.println("No skills directory found. Nothing to update.");
                return 0;
            }

            int updated = 0;
            int failed = 0;
            int upToDate = 0;

            var skillDirs = Files.list(skillsDir)
                    .filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .toList();

            if (skillDirs.isEmpty()) {
                System.out.println("No installed skills found.");
                return 0;
            }

            System.out.println("Checking " + skillDirs.size() + " installed skills for updates...\n");

            for (Path skillDir : skillDirs) {
                String slug = skillDir.getFileName().toString();
                try {
                    SkillInfo localInfo = readLocalSkillInfo(skillDir);
                    SkillInfo remoteInfo = apiClient.getSkillInfo(slug);

                    if (localInfo == null || remoteInfo == null) {
                        System.err.println("  ✗ " + slug + " - Could not check for updates");
                        failed++;
                        continue;
                    }

                    String localVersion = localInfo.getVersion();
                    String remoteVersion = remoteInfo.getLatestVersion();

                    if (localVersion.equals(remoteVersion)) {
                        System.out.println("  ✓ " + slug + " - Already up to date (" + localVersion + ")");
                        upToDate++;
                    } else {
                        if (dryRun) {
                            System.out.println("  → " + slug + " - Would update " + localVersion + " → " + remoteVersion);
                        } else {
                            System.out.println("  → " + slug + " - Updating " + localVersion + " → " + remoteVersion);
                            if (updateSkill(apiClient, skillsDir, slug) == 0) {
                                updated++;
                            } else {
                                failed++;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("  ✗ " + slug + " - " + e.getMessage());
                    failed++;
                }
            }

            System.out.println("\nUpdate complete: " + updated + " updated, " + upToDate + " up to date, " + failed + " failed");
            return failed > 0 ? 1 : 0;

        } catch (IOException e) {
            System.err.println("Error reading skills directory: " + e.getMessage());
            return 1;
        }
    }

    private SkillInfo readLocalSkillInfo(Path skillDir) throws IOException {
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            return null;
        }

        String content = Files.readString(skillMd);
        SkillInfo info = new SkillInfo();
        info.setSlug(skillDir.getFileName().toString());
        
        // Parse version from frontmatter or metadata
        // This is a simplified version - real implementation would parse YAML frontmatter
        if (content.contains("version:")) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("version:")) {
                    info.setVersion(line.split(":")[1].trim().replace("\"", "").replace("'", ""));
                    break;
                }
            }
        }
        
        if (info.getVersion() == null) {
            info.setVersion("0.0.0"); // Unknown version
        }
        
        return info;
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