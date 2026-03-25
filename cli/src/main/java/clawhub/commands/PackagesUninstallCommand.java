package clawhub.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
    name = "uninstall",
    description = "Uninstall a package",
    mixinStandardHelpOptions = true
)
public class PackagesUninstallCommand implements Callable<Integer> {
    
    @Parameters(paramLabel = "<name>", description = "Package name")
    private String name;
    
    @Option(names = {"-d", "--directory"}, description = "Installation directory (default: ./packages)")
    private String directory = "./packages";
    
    @Option(names = {"--force"}, description = "Force uninstall without confirmation")
    private boolean force;
    
    @Option(names = {"--dry-run"}, description = "Show what would be uninstalled")
    private boolean dryRun;
    
    @Override
    public Integer call() throws Exception {
        Path installDir = Path.of(directory).toAbsolutePath().normalize().resolve(name);
        
        if (!Files.exists(installDir)) {
            System.err.println("Error: Package not installed: " + name);
            return 1;
        }
        
        // Check if it's a valid package
        Path metaPath = installDir.resolve(".clawhub-package.json");
        if (!Files.exists(metaPath)) {
            System.err.println("Warning: Directory exists but doesn't appear to be a ClawHub package");
            if (!force) {
                System.err.println("Use --force to remove anyway.");
                return 1;
            }
        }
        
        if (dryRun) {
            System.out.println("Dry run - would uninstall:");
            System.out.println("  Package: " + name);
            System.out.println("  From: " + installDir);
            return 0;
        }
        
        // Count files
        long fileCount;
        try (Stream<Path> files = Files.walk(installDir)) {
            fileCount = files.filter(Files::isRegularFile).count();
        }
        
        System.out.println("Uninstalling " + name + "...");
        System.out.println("Removing " + fileCount + " files from " + installDir);
        
        deleteDirectory(installDir);
        
        System.out.println();
        System.out.println("✓ Successfully uninstalled " + name);
        
        return 0;
    }
    
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                stream.forEach(child -> {
                    try {
                        deleteDirectory(child);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.delete(dir);
    }
}
