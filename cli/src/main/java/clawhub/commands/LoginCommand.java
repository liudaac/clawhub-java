package clawhub.commands;

import clawhub.config.CliConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;

@Command(
    name = "login",
    description = "Login to ClawHub via GitHub OAuth"
)
public class LoginCommand implements Callable<Integer> {
    
    @Option(names = {"--server"}, description = "ClawHub server URL", defaultValue = "http://localhost:8080")
    private String serverUrl;
    
    @Override
    public Integer call() throws Exception {
        String authUrl = serverUrl + "/oauth2/authorization/github";
        
        System.out.println("Opening browser for GitHub authentication...");
        System.out.println("If the browser doesn't open, visit:");
        System.out.println(authUrl);
        
        // Try to open browser
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(URI.create(authUrl));
            } catch (IOException e) {
                System.err.println("Could not open browser: " + e.getMessage());
            }
        }
        
        System.out.println("\nAfter authentication, the token will be displayed in the browser.");
        System.out.println("Use 'clawhub whoami' to verify your login.");
        
        return 0;
    }
}
