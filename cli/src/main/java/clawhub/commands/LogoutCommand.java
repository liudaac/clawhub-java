package clawhub.commands;

import clawhub.config.CliConfig;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
    name = "logout",
    description = "Logout from ClawHub"
)
public class LogoutCommand implements Callable<Integer> {
    
    @Override
    public Integer call() throws Exception {
        CliConfig config = CliConfig.load();
        
        if (config.getToken() == null) {
            System.out.println("You are not logged in.");
            return 0;
        }
        
        config.setToken(null);
        config.save();
        
        System.out.println("Successfully logged out.");
        return 0;
    }
}
