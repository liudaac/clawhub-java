package clawhub.commands;

import clawhub.api.ClawhubApiClient;
import clawhub.config.CliConfig;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "star",
    description = "Star a skill"
)
public class StarCommand implements Callable<Integer> {

    @CommandLine.Parameters(paramLabel = "<skill>", description = "Skill slug to star")
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
            apiClient.starSkill(skillSlug);
            System.out.println("✓ Starred '" + skillSlug + "'");
            return 0;
        } catch (Exception e) {
            System.err.println("Error starring skill: " + e.getMessage());
            return 1;
        }
    }
}

@CommandLine.Command(
    name = "unstar",
    description = "Unstar a skill"
)
class UnstarCommand implements Callable<Integer> {

    @CommandLine.Parameters(paramLabel = "<skill>", description = "Skill slug to unstar")
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
            apiClient.unstarSkill(skillSlug);
            System.out.println("✓ Unstarred '" + skillSlug + "'");
            return 0;
        } catch (Exception e) {
            System.err.println("Error unstarring skill: " + e.getMessage());
            return 1;
        }
    }
}
