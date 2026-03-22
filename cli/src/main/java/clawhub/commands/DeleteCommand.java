package clawhub.commands;

import clawhub.api.ClawhubApiClient;
import clawhub.config.CliConfig;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "delete",
    description = "Delete a skill from the registry",
    aliases = {"del"}
)
public class DeleteCommand implements Callable<Integer> {

    @CommandLine.Parameters(paramLabel = "<skill>", description = "Skill slug to delete")
    private String skillSlug;

    @CommandLine.Option(names = {"-f", "--force"}, description = "Force delete without confirmation")
    private boolean force;

    @CommandLine.Option(names = {"--permanent"}, description = "Permanently delete (cannot be undone)")
    private boolean permanent;

    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        if (!config.isAuthenticated()) {
            System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
            return 1;
        }

        // Confirm delete
        if (!force) {
            String action = permanent ? "permanently delete" : "delete";
            System.out.print("Are you sure you want to " + action + " '" + skillSlug + "'? [y/N] ");
            String response = System.console().readLine();
            if (!response.equalsIgnoreCase("y") && !response.equalsIgnoreCase("yes")) {
                System.out.println("Delete cancelled.");
                return 0;
            }
        }

        try {
            ClawhubApiClient apiClient = new ClawhubApiClient(config);
            
            if (permanent) {
                apiClient.permanentlyDeleteSkill(skillSlug);
                System.out.println("✓ Skill '" + skillSlug + "' permanently deleted");
            } else {
                apiClient.deleteSkill(skillSlug);
                System.out.println("✓ Skill '" + skillSlug + "' deleted (can be restored)");
            }
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error deleting skill: " + e.getMessage());
            return 1;
        }
    }
}

@CommandLine.Command(
    name = "hide",
    description = "Hide a skill from public view"
)
class HideCommand implements Callable<Integer> {

    @CommandLine.Parameters(paramLabel = "<skill>", description = "Skill slug to hide")
    private String skillSlug;

    @CommandLine.Option(names = {"-r", "--reason"}, description = "Reason for hiding")
    private String reason;

    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        if (!config.isAuthenticated()) {
            System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
            return 1;
        }

        try {
            ClawhubApiClient apiClient = new ClawhubApiClient(config);
            apiClient.hideSkill(skillSlug, reason);
            System.out.println("✓ Skill '" + skillSlug + "' is now hidden");
            return 0;
        } catch (Exception e) {
            System.err.println("Error hiding skill: " + e.getMessage());
            return 1;
        }
    }
}

@CommandLine.Command(
    name = "unhide",
    description = "Unhide a previously hidden skill"
)
class UnhideCommand implements Callable<Integer> {

    @CommandLine.Parameters(paramLabel = "<skill>", description = "Skill slug to unhide")
    private String skillSlug;

    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        if (!config.isAuthenticated()) {
            System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
            return 1;
        }

        try {
            ClawhubApiClient apiClient = new ClawhubApiClient(config);
            apiClient.unhideSkill(skillSlug);
            System.out.println("✓ Skill '" + skillSlug + "' is now visible");
            return 0;
        } catch (Exception e) {
            System.err.println("Error unhiding skill: " + e.getMessage());
            return 1;
        }
    }
}

@CommandLine.Command(
    name = "undelete",
    description = "Restore a deleted skill"
)
class UndeleteCommand implements Callable<Integer> {

    @CommandLine.Parameters(paramLabel = "<skill>", description = "Skill slug to restore")
    private String skillSlug;

    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        if (!config.isAuthenticated()) {
            System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
            return 1;
        }

        try {
            ClawhubApiClient apiClient = new ClawhubApiClient(config);
            apiClient.undeleteSkill(skillSlug);
            System.out.println("✓ Skill '" + skillSlug + "' has been restored");
            return 0;
        } catch (Exception e) {
            System.err.println("Error restoring skill: " + e.getMessage());
            return 1;
        }
    }
}
