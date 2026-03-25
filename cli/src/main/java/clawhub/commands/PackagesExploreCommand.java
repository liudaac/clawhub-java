package clawhub.commands;

import clawhub.api.ClawhubApi;
import clawhub.config.CliConfig;
import clawhub.model.Package;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "explore",
    description = "Browse and search packages",
    mixinStandardHelpOptions = true
)
public class PackagesExploreCommand implements Callable<Integer> {
    
    @Parameters(paramLabel = "<query>", description = "Search query (optional)", arity = "0..1")
    private String query;
    
    @Option(names = {"-f", "--family"}, description = "Filter by family: skill, code-plugin, bundle-plugin")
    private String family;
    
    @Option(names = {"-c", "--channel"}, description = "Filter by channel: official, community, private")
    private String channel;
    
    @Option(names = {"--official"}, description = "Show only official packages")
    private boolean official;
    
    @Option(names = {"--runtime"}, description = "Filter by runtime ID")
    private String runtimeId;
    
    @Option(names = {"-l", "--limit"}, description = "Maximum number of results", defaultValue = "20")
    private int limit;
    
    @Option(names = {"-p", "--page"}, description = "Page number", defaultValue = "0")
    private int page;
    
    private final CliConfig config = CliConfig.load();
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public Integer call() throws Exception {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(config.getServerUrl() + "/api/v1/packages")
                .newBuilder();
            
            if (query != null && !query.isEmpty()) {
                urlBuilder.addQueryParameter("search", query);
            }
            if (family != null) {
                urlBuilder.addQueryParameter("family", family.toLowerCase().replace("-", "_"));
            }
            if (channel != null) {
                urlBuilder.addQueryParameter("channel", channel.toLowerCase());
            }
            if (official) {
                urlBuilder.addQueryParameter("official", "true");
            }
            if (runtimeId != null) {
                urlBuilder.addQueryParameter("runtimeId", runtimeId);
            }
            urlBuilder.addQueryParameter("size", String.valueOf(limit));
            urlBuilder.addQueryParameter("page", String.valueOf(page));
            
            Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Error: " + response.code() + " - " + response.message());
                    return 1;
                }
                
                JsonNode root = mapper.readTree(response.body().string());
                JsonNode packagesNode = root.get("packages");
                
                if (packagesNode == null || !packagesNode.isArray() || packagesNode.size() == 0) {
                    System.out.println("No packages found.");
                    return 0;
                }
                
                List<Package> packages = mapper.treeToValue(packagesNode,
                    mapper.getTypeFactory().constructCollectionType(List.class, Package.class));
                
                printPackages(packages);
                
                long total = root.has("total") ? root.get("total").asLong() : packages.size();
                boolean hasMore = root.has("hasMore") ? root.get("hasMore").asBoolean() : false;
                
                System.out.println();
                System.out.println("Showing " + packages.size() + " of " + total + " packages");
                if (hasMore) {
                    System.out.println("Use --page " + (page + 1) + " to see more results");
                }
                
                return 0;
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    private void printPackages(List<Package> packages) {
        System.out.printf("%-30s %-15s %-12s %-10s %-8s%n", 
            "NAME", "FAMILY", "CHANNEL", "VERSION", "STARS");
        System.out.println("-".repeat(80));
        
        for (Package pkg : packages) {
            String name = truncate(pkg.getDisplayName(), 28);
            String family = truncate(pkg.getFamily(), 13);
            String channel = truncate(pkg.getChannel(), 10);
            String version = truncate(pkg.getLatestVersion(), 8);
            String stars = pkg.getStatsStars() != null ? String.valueOf(pkg.getStatsStars()) : "0";
            
            String officialMark = pkg.isOfficial() ? "✓" : " ";
            System.out.printf("%-30s %-15s %-12s %-10s %s%-7s%n", 
                name, family, channel, version, officialMark, stars);
        }
    }
    
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
