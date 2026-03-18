package clawhub.commands;

import clawhub.api.ClawhubApi;
import clawhub.config.CliConfig;
import clawhub.model.User;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
    name = "whoami",
    description = "Show current user information"
)
public class WhoamiCommand implements Callable<Integer> {
    
    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        
        if (config.getToken() == null) {
            System.err.println("Error: Not logged in. Run 'clawhub login' first.");
            return 1;
        }
        
        ClawhubApi api = new ClawhubApi(config);
        
        try {
            User user = api.whoami();
            
            System.out.println("Logged in as:");
            System.out.println("  Handle: @" + user.getHandle());
            System.out.println("  Name: " + (user.getName() != null ? user.getName() : "N/A"));
            System.out.println("  Role: " + user.getRole());
            
            if (user.getBio() != null) {
                System.out.println("  Bio: " + user.getBio());
            }
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Your session may have expired. Run 'clawhub login' again.");
            return 1;
        }
    }
}
