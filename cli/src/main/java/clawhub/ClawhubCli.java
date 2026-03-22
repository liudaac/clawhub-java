package clawhub;

import clawhub.commands.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "clawhub",
    description = "ClawHub CLI - Manage skills and souls",
    mixinStandardHelpOptions = true,
    version = "clawhub 1.0.0",
    subcommands = {
        LoginCommand.class,
        LogoutCommand.class,
        WhoamiCommand.class,
        SearchCommand.class,
        InstallCommand.class,
        ListCommand.class,
        PublishCommand.class,
        SyncCommand.class,
        UpdateCommand.class,
        UninstallCommand.class,
        DeleteCommand.class,
        HideCommand.class,
        UnhideCommand.class,
        UndeleteCommand.class,
        InspectCommand.class,
        TransferCommand.class,
        StarCommand.class,
        UnstarCommand.class
    }
)
public class ClawhubCli implements Runnable {
    
    @Spec
    CommandSpec spec;
    
    @Override
    public void run() {
        // If no subcommand is specified, show help
        spec.commandLine().usage(System.out);
    }
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ClawhubCli()).execute(args);
        System.exit(exitCode);
    }
}
