package clawhub.commands;

import picocli.CommandLine.Command;

@Command(
    name = "packages",
    description = "Manage packages (skills, plugins, bundles)",
    mixinStandardHelpOptions = true,
    subcommands = {
        PackagesExploreCommand.class,
        PackagesInspectCommand.class,
        PackagesPublishCommand.class,
        PackagesInstallCommand.class,
        PackagesUninstallCommand.class,
        PackagesListCommand.class
    }
)
public class PackagesCommand implements Runnable {
    
    @Override
    public void run() {
        // Show help if no subcommand
        System.out.println("Use 'clawhub packages <command>' to manage packages.");
        System.out.println("Available commands:");
        System.out.println("  explore    Browse and search packages");
        System.out.println("  inspect    View package details");
        System.out.println("  publish    Publish a new package");
        System.out.println("  install    Install a package");
        System.out.println("  uninstall  Uninstall a package");
        System.out.println("  list       List installed packages");
    }
}
