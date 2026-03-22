package clawhub.commands;

import clawhub.api.ClawhubApiClient;
import clawhub.config.CliConfig;
import clawhub.model.TransferRequest;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "transfer",
    description = "Manage skill ownership transfers",
    subcommands = {
        TransferCommand.RequestCommand.class,
        TransferCommand.ListCommand.class,
        TransferCommand.AcceptCommand.class,
        TransferCommand.RejectCommand.class,
        TransferCommand.CancelCommand.class
    }
)
public class TransferCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        // Show help if no subcommand
        return 0;
    }

    @CommandLine.Command(
        name = "request",
        description = "Request to transfer skill ownership to another user"
    )
    static class RequestCommand implements Callable<Integer> {

        @CommandLine.Parameters(paramLabel = "<skill>", description = "Skill slug to transfer")
        private String skillSlug;

        @CommandLine.Parameters(paramLabel = "<user>", description = "Username to transfer to")
        private String toUser;

        @CommandLine.Option(names = {"-m", "--message"}, description = "Transfer message")
        private String message;

        @Override
        public Integer call() throws Exception {
            CliConfig config = CliConfig.load();
            if (!config.isAuthenticated()) {
                System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
                return 1;
            }

            try {
                ClawhubApiClient apiClient = new ClawhubApiClient(config);
                apiClient.requestTransfer(skillSlug, toUser, message);
                System.out.println("✓ Transfer request sent for '" + skillSlug + "' to @" + toUser);
                System.out.println("  The recipient will need to accept the transfer.");
                return 0;
            } catch (Exception e) {
                System.err.println("Error requesting transfer: " + e.getMessage());
                return 1;
            }
        }
    }

    @CommandLine.Command(
        name = "list",
        description = "List pending transfer requests",
        aliases = {"ls"}
    )
    static class ListCommand implements Callable<Integer> {

        @CommandLine.Option(names = {"--incoming"}, description = "Show only incoming requests")
        private boolean incoming;

        @CommandLine.Option(names = {"--outgoing"}, description = "Show only outgoing requests")
        private boolean outgoing;

        @Override
        public Integer call() throws Exception {
            CliConfig config = CliConfig.load();
            if (!config.isAuthenticated()) {
                System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
                return 1;
            }

            try {
                ClawhubApiClient apiClient = new ClawhubApiClient(config);
                
                if (!outgoing) {
                    List<TransferRequest> incomingRequests = apiClient.getIncomingTransfers();
                    if (!incomingRequests.isEmpty()) {
                        System.out.println("Incoming transfer requests:");
                        System.out.println("─────────────────────────────────────────");
                        for (TransferRequest req : incomingRequests) {
                            System.out.println("Skill: " + req.getSkillSlug());
                            System.out.println("From: @" + req.getFromUser());
                            System.out.println("Requested: " + req.getCreatedAt());
                            if (req.getMessage() != null) {
                                System.out.println("Message: " + req.getMessage());
                            }
                            System.out.println("  Run: clawhub transfer accept " + req.getId());
                            System.out.println("─────────────────────────────────────────");
                        }
                    }
                }

                if (!incoming) {
                    List<TransferRequest> outgoingRequests = apiClient.getOutgoingTransfers();
                    if (!outgoingRequests.isEmpty()) {
                        System.out.println("\nOutgoing transfer requests:");
                        System.out.println("─────────────────────────────────────────");
                        for (TransferRequest req : outgoingRequests) {
                            System.out.println("Skill: " + req.getSkillSlug());
                            System.out.println("To: @" + req.getToUser());
                            System.out.println("Requested: " + req.getCreatedAt());
                            System.out.println("Status: " + req.getStatus());
                            System.out.println("─────────────────────────────────────────");
                        }
                    }
                }

                return 0;
            } catch (Exception e) {
                System.err.println("Error listing transfers: " + e.getMessage());
                return 1;
            }
        }
    }

    @CommandLine.Command(
        name = "accept",
        description = "Accept a transfer request"
    )
    static class AcceptCommand implements Callable<Integer> {

        @CommandLine.Parameters(paramLabel = "<id>", description = "Transfer request ID")
        private String transferId;

        @CommandLine.Option(names = {"-m", "--message"}, description = "Response message")
        private String message;

        @Override
        public Integer call() throws Exception {
            CliConfig config = CliConfig.load();
            if (!config.isAuthenticated()) {
                System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
                return 1;
            }

            try {
                ClawhubApiClient apiClient = new ClawhubApiClient(config);
                apiClient.acceptTransfer(transferId, message);
                System.out.println("✓ Transfer request accepted");
                System.out.println("  You are now the owner of this skill.");
                return 0;
            } catch (Exception e) {
                System.err.println("Error accepting transfer: " + e.getMessage());
                return 1;
            }
        }
    }

    @CommandLine.Command(
        name = "reject",
        description = "Reject a transfer request"
    )
    static class RejectCommand implements Callable<Integer> {

        @CommandLine.Parameters(paramLabel = "<id>", description = "Transfer request ID")
        private String transferId;

        @CommandLine.Option(names = {"-m", "--message"}, description = "Response message")
        private String message;

        @Override
        public Integer call() throws Exception {
            CliConfig config = CliConfig.load();
            if (!config.isAuthenticated()) {
                System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
                return 1;
            }

            try {
                ClawhubApiClient apiClient = new ClawhubApiClient(config);
                apiClient.rejectTransfer(transferId, message);
                System.out.println("✓ Transfer request rejected");
                return 0;
            } catch (Exception e) {
                System.err.println("Error rejecting transfer: " + e.getMessage());
                return 1;
            }
        }
    }

    @CommandLine.Command(
        name = "cancel",
        description = "Cancel an outgoing transfer request"
    )
    static class CancelCommand implements Callable<Integer> {

        @CommandLine.Parameters(paramLabel = "<id>", description = "Transfer request ID")
        private String transferId;

        @Override
        public Integer call() throws Exception {
            CliConfig config = CliConfig.load();
            if (!config.isAuthenticated()) {
                System.err.println("Error: Not authenticated. Run 'clawhub login' first.");
                return 1;
            }

            try {
                ClawhubApiClient apiClient = new ClawhubApiClient(config);
                apiClient.cancelTransfer(transferId);
                System.out.println("✓ Transfer request cancelled");
                return 0;
            } catch (Exception e) {
                System.err.println("Error cancelling transfer: " + e.getMessage());
                return 1;
            }
        }
    }
}
