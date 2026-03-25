package clawhub.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
    name = "list",
    description = "List installed packages",
    mixinStandardHelpOptions = true
)
public class PackagesListCommand implements Callable<Integer> {
    
    @Option(names = {"-d", "--directory"}, description = "Installation directory (default: ./packages)")
    private String directory = "./packages";
    
    @Option(names = {"--json"}, description = "Output as JSON")
    private boolean json;
    
    @Option(names = {"--outdated"}, description = "Show only outdated packages")
    private boolean outdated;
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public Integer call() throws Exception {
        Path packagesDir = Path.of(directory).toAbsolutePath().normalize();
        
        if (!Files.exists(packagesDir)) {
            System.out.println("No packages installed.");
            return 0;
        }
        
        List<InstalledPackage> packages = new ArrayList<>();
        
        try (Stream<Path> stream = Files.list(packagesDir)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                try {
                    Path metaPath = dir.resolve(".clawhub-package.json");
                    if (Files.exists(metaPath)) {
                        String content = Files.readString(metaPath);
                        JsonNode meta = mapper.readTree(content);
                        
                        InstalledPackage pkg = new InstalledPackage();
                        pkg.name = meta.has("name") ? meta.get("name").asText() : dir.getFileName().toString();
                        pkg.version = meta.has("version") ? meta.get("version").asText() : "unknown";
                        pkg.source = meta.has("source") ? meta.get("source").asText() : "unknown";
                        pkg.installedAt = meta.has("installedAt") ? meta.get("installedAt").asText() : "unknown";
                        pkg.path = dir.toString();
                        packages.add(pkg);
                    }
                } catch (IOException e) {
                    // Skip invalid packages
                }
            });
        }
        
        if (packages.isEmpty()) {
            System.out.println("No packages installed.");
            return 0;
        }
        
        if (json) {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(packages));
        } else {
            printHumanReadable(packages);
        }
        
        return 0;
    }
    
    private void printHumanReadable(List<InstalledPackage> packages) {
        System.out.printf("%-30s %-15s %-30s%n", "NAME", "VERSION", "INSTALLED");
        System.out.println("-".repeat(80));
        
        for (InstalledPackage pkg : packages) {
            String name = truncate(pkg.name, 28);
            String version = truncate(pkg.version, 13);
            String installed = truncate(pkg.installedAt, 28);
            System.out.printf("%-30s %-15s %-30s%n", name, version, installed);
        }
        
        System.out.println();
        System.out.println("Total: " + packages.size() + " package(s)");
    }
    
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
    
    public static class InstalledPackage {
        public String name;
        public String version;
        public String source;
        public String installedAt;
        public String path;
    }
}
