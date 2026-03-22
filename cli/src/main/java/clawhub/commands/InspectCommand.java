package clawhub.commands;

import clawhub.api.ClawhubApiClient;
import clawhub.config.CliConfig;
import clawhub.model.SkillDetail;
import clawhub.model.SkillVersionInfo;
import picocli.CommandLine;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "inspect",
    description = "Show detailed information about a skill",
    aliases = {"info", "show"}
)
public class InspectCommand implements Callable<Integer> {

    @CommandLine.Parameters(paramLabel = "<skill>", description = "Skill slug to inspect")
    private String skillSlug;

    @CommandLine.Option(names = {"-v", "--version"}, description = "Specific version to inspect")
    private String version;

    @CommandLine.Option(names = {"--json"}, description = "Output in JSON format")
    private boolean json;

    @CommandLine.Option(names = {"--files"}, description = "Show file list")
    private boolean showFiles;

    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        ClawhubApiClient apiClient = new ClawhubApiClient(config);

        try {
            SkillDetail skill;
            if (version != null) {
                skill = apiClient.getSkillVersion(skillSlug, version);
            } else {
                skill = apiClient.getSkillDetail(skillSlug);
            }

            if (skill == null) {
                System.err.println("Error: Skill '" + skillSlug + "' not found.");
                return 1;
            }

            if (json) {
                printJson(skill);
            } else {
                printFormatted(skill);
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Error inspecting skill: " + e.getMessage());
            return 1;
        }
    }

    private void printFormatted(SkillDetail skill) {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│ Skill: " + padRight(skill.getSlug(), 33) + "│");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│ Name:        " + padRight(skill.getDisplayName(), 28) + "│");
        System.out.println("│ Owner:       " + padRight(skill.getOwnerHandle(), 28) + "│");
        System.out.println("│ Version:     " + padRight(skill.getLatestVersion(), 28) + "│");
        System.out.println("│ Downloads:   " + padRight(String.valueOf(skill.getDownloads()), 28) + "│");
        System.out.println("│ Stars:       " + padRight(String.valueOf(skill.getStars()), 28) + "│");
        System.out.println("│ Status:      " + padRight(skill.getStatus(), 28) + "│");
        System.out.println("├─────────────────────────────────────────┤");
        
        if (skill.getSummary() != null) {
            System.out.println("│ Summary:                                │");
            String summary = skill.getSummary();
            if (summary.length() > 40) {
                summary = summary.substring(0, 37) + "...";
            }
            System.out.println("│ " + padRight(summary, 39) + "│");
        }
        
        if (skill.getLicense() != null) {
            System.out.println("│ License:     " + padRight(skill.getLicense(), 28) + "│");
        }
        
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│ Versions:                               │");
        
        if (skill.getVersions() != null && !skill.getVersions().isEmpty()) {
            for (SkillVersionInfo ver : skill.getVersions().subList(0, Math.min(5, skill.getVersions().size()))) {
                String verStr = "  " + ver.getVersion();
                if (ver.getCreatedAt() != null) {
                    verStr += " (" + ver.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE) + ")";
                }
                System.out.println("│ " + padRight(verStr, 39) + "│");
            }
            if (skill.getVersions().size() > 5) {
                System.out.println("│ " + padRight("  ... and " + (skill.getVersions().size() - 5) + " more", 39) + "│");
            }
        }
        
        System.out.println("└─────────────────────────────────────────┘");

        // Security info
        if (skill.getModerationVerdict() != null) {
            System.out.println("\nSecurity:");
            System.out.println("  Verdict: " + skill.getModerationVerdict());
            if (skill.getModerationFlags() != null && !skill.getModerationFlags().isEmpty()) {
                System.out.println("  Flags: " + String.join(", ", skill.getModerationFlags()));
            }
        }

        // Files
        if (showFiles && skill.getFiles() != null) {
            System.out.println("\nFiles:");
            for (var file : skill.getFiles()) {
                String size = formatFileSize(file.getSize());
                System.out.println("  " + padRight(file.getPath(), 40) + " " + size);
            }
        }
    }

    private void printJson(SkillDetail skill) {
        // Simplified JSON output - real implementation would use Jackson
        System.out.println("{");
        System.out.println("  \"slug\": \"" + skill.getSlug() + "\",");
        System.out.println("  \"displayName\": \"" + skill.getDisplayName() + "\",");
        System.out.println("  \"owner\": \"" + skill.getOwnerHandle() + "\",");
        System.out.println("  \"version\": \"" + skill.getLatestVersion() + "\",");
        System.out.println("  \"downloads\": " + skill.getDownloads() + ",");
        System.out.println("  \"stars\": " + skill.getStars() + ",");
        System.out.println("  \"status\": \"" + skill.getStatus() + "\"");
        System.out.println("}");
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() > n) return s.substring(0, n);
        return String.format("%-" + n + "s", s);
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }
}
