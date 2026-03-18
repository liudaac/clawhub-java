package clawhub.commands;

import clawhub.api.ClawhubApi;
import clawhub.config.CliConfig;
import clawhub.model.Skill;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "search",
    description = "Search for skills and souls"
)
public class SearchCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Search query")
    private String query;
    
    @Option(names = {"-t", "--type"}, description = "Filter by type: skills, souls, all", defaultValue = "all")
    private String type;
    
    @Option(names = {"-l", "--limit"}, description = "Maximum number of results", defaultValue = "20")
    private int limit;
    
    @Option(names = {"--server"}, description = "ClawHub server URL", defaultValue = "http://localhost:8080")
    private String serverUrl;
    
    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        config.setServerUrl(serverUrl);
        
        ClawhubApi api = new ClawhubApi(config);
        
        try {
            System.out.println("Searching for: " + query);
            System.out.println();
            
            if (type.equals("all") || type.equals("skills")) {
                List<Skill> skills = api.searchSkills(query, limit);
                
                if (!skills.isEmpty()) {
                    System.out.println("Skills (" + skills.size() + "):");
                    for (Skill skill : skills) {
                        System.out.println("  " + skill.getSlug());
                        System.out.println("    " + skill.getDisplayName());
                        System.out.println("    Downloads: " + skill.getStatsDownloads() + 
                                         " | Stars: " + skill.getStatsStars());
                        System.out.println();
                    }
                }
            }
            
            // TODO: Search souls
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
