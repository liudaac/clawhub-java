package clawhub.api;

import clawhub.config.CliConfig;
import clawhub.model.Skill;
import clawhub.model.SkillVersion;
import clawhub.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

public class ClawhubApi {
    
    private final CliConfig config;
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    
    public ClawhubApi(CliConfig config) {
        this.config = config;
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }
    
    public User whoami() throws IOException {
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/auth/whoami")
            .header("Authorization", "Bearer " + config.getToken())
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            
            JsonNode root = mapper.readTree(response.body().string());
            return mapper.treeToValue(root.get("data"), User.class);
        }
    }
    
    public List<Skill> searchSkills(String query, int limit) throws IOException {
        HttpUrl url = HttpUrl.parse(config.getServerUrl() + "/api/search/skills")
            .newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("size", String.valueOf(limit))
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            
            JsonNode root = mapper.readTree(response.body().string());
            return mapper.treeToValue(root.get("data"), 
                mapper.getTypeFactory().constructCollectionType(List.class, Skill.class));
        }
    }
    
    public Skill getSkill(String slug) throws IOException {
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/skills/" + slug)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    return null;
                }
                throw new IOException("Unexpected code " + response);
            }
            
            JsonNode root = mapper.readTree(response.body().string());
            return mapper.treeToValue(root.get("data"), Skill.class);
        }
    }
    
    public SkillVersion getSkillVersion(String slug, String version) throws IOException {
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/skills/" + slug + "/versions/" + version)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            
            JsonNode root = mapper.readTree(response.body().string());
            return mapper.treeToValue(root.get("data"), SkillVersion.class);
        }
    }
    
    public byte[] downloadFile(String storageId) throws IOException {
        // This would download from MinIO or file service
        // Simplified implementation
        throw new UnsupportedOperationException("Not implemented");
    }
    
    public Skill createSkill(String slug, String displayName, String summary) throws IOException {
        JsonNode body = mapper.createObjectNode()
            .put("slug", slug)
            .put("displayName", displayName)
            .put("summary", summary);
        
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/skills")
            .header("Authorization", "Bearer " + config.getToken())
            .post(RequestBody.create(mapper.writeValueAsString(body), 
                MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            
            JsonNode root = mapper.readTree(response.body().string());
            return mapper.treeToValue(root.get("data"), Skill.class);
        }
    }
    
    public String uploadFile(byte[] content, String filename) throws IOException {
        // Upload to file service
        // Simplified implementation
        return "storage-id-placeholder";
    }
    
    public void createVersion(String slug, String version, String changelog, ArrayNode files) throws IOException {
        JsonNode body = mapper.createObjectNode()
            .put("version", version)
            .put("changelog", changelog)
            .set("files", files);
        
        Request request = new Request.Builder()
            .url(config.getServerUrl() + "/api/skills/" + slug + "/versions")
            .header("Authorization", "Bearer " + config.getToken())
            .post(RequestBody.create(mapper.writeValueAsString(body), 
                MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
        }
    }
}
