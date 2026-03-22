package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.dto.FileContentResponse;
import clawhub.entity.Skill;
import clawhub.entity.SkillVersion;
import clawhub.exception.ResourceNotFoundException;
import clawhub.service.SkillService;
import clawhub.service.SkillVersionService;
import clawhub.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/v1/skills/{skillSlug}")
@RequiredArgsConstructor
@Tag(name = "Skill Files", description = "技能文件查看")
public class SkillFileController {

    private final SkillService skillService;
    private final SkillVersionService versionService;
    private final StorageService storageService;

    @GetMapping("/versions/{version}/files")
    @Operation(summary = "获取版本文件列表")
    public ResponseEntity<ApiResponse<List<SkillVersion.FileInfo>>> listFiles(
            @PathVariable String skillSlug,
            @PathVariable String version) {
        Skill skill = skillService.findBySlug(skillSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillSlug));
        
        SkillVersion skillVersion = versionService.findBySkillAndVersion(skill, version)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + version));
        
        return ResponseEntity.ok(ApiResponse.success(skillVersion.getFiles()));
    }

    @GetMapping("/versions/{version}/files/{*path}")
    @Operation(summary = "获取文件内容")
    public ResponseEntity<ApiResponse<FileContentResponse>> getFileContent(
            @PathVariable String skillSlug,
            @PathVariable String version,
            @PathVariable String path) {
        
        Skill skill = skillService.findBySlug(skillSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillSlug));
        
        SkillVersion skillVersion = versionService.findBySkillAndVersion(skill, version)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + version));
        
        // 移除前导斜杠
        String filePath = path.startsWith("/") ? path.substring(1) : path;
        
        byte[] content = storageService.readFileBytes(skill.getId(), skillVersion.getId(), filePath);
        
        String contentType = detectContentType(filePath);
        boolean isText = isTextFile(contentType);
        boolean isImage = contentType.startsWith("image/");
        boolean isBinary = !isText && !isImage;
        
        FileContentResponse response = FileContentResponse.builder()
                .path(filePath)
                .content(isText ? new String(content, StandardCharsets.UTF_8) : Base64.getEncoder().encodeToString(content))
                .contentType(contentType)
                .size(content.length)
                .isText(isText)
                .isImage(isImage)
                .isBinary(isBinary)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/versions/{version}/files/{*path}/raw")
    @Operation(summary = "获取文件原始内容")
    public ResponseEntity<byte[]> getFileRaw(
            @PathVariable String skillSlug,
            @PathVariable String version,
            @PathVariable String path) {
        
        Skill skill = skillService.findBySlug(skillSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillSlug));
        
        SkillVersion skillVersion = versionService.findBySkillAndVersion(skill, version)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + version));
        
        String filePath = path.startsWith("/") ? path.substring(1) : path;
        byte[] content = storageService.readFileBytes(skill.getId(), skillVersion.getId(), filePath);
        
        String contentType = detectContentType(filePath);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(content);
    }

    @GetMapping("/versions/{version}/diff")
    @Operation(summary = "获取版本差异对比")
    public ResponseEntity<ApiResponse<String>> getVersionDiff(
            @PathVariable String skillSlug,
            @PathVariable String version,
            @RequestParam String compareWith) {
        
        // 这里应该实现版本对比逻辑
        // 简化版本，返回占位符
        return ResponseEntity.ok(ApiResponse.success("Diff not yet implemented"));
    }

    private String detectContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "text/markdown";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "text/yaml";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".ts")) return "application/typescript";
        if (lower.endsWith(".html")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".py")) return "text/x-python";
        if (lower.endsWith(".sh")) return "text/x-shellscript";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }

    private boolean isTextFile(String contentType) {
        return contentType.startsWith("text/") || 
               contentType.equals("application/json") ||
               contentType.equals("application/javascript") ||
               contentType.equals("application/typescript") ||
               contentType.equals("text/yaml") ||
               contentType.equals("text/x-python") ||
               contentType.equals("text/x-shellscript");
    }
}
