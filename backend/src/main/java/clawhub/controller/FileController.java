package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.service.StorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("ownerId") UUID ownerId,
            @RequestParam("slug") String slug) {
        
        try {
            String path = storageService.generateStoragePath(type, ownerId, slug);
            String objectName = storageService.uploadFile(file, path);
            String url = storageService.getPresignedUrl(objectName);
            
            Map<String, String> response = new HashMap<>();
            response.put("objectName", objectName);
            response.put("url", url);
            response.put("originalName", file.getOriginalFilename());
            response.put("size", String.valueOf(file.getSize()));
            
            return ResponseEntity.ok(ApiResponse.success(response, "File uploaded successfully"));
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public void downloadFile(
            @RequestParam("objectName") String objectName,
            HttpServletResponse response) {
        
        try {
            InputStream stream = storageService.downloadFile(objectName);
            
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"");
            
            stream.transferTo(response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            log.error("Failed to download file: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam("objectName") String objectName) {
        
        try {
            String url = storageService.getPresignedUrl(objectName);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            response.put("objectName", objectName);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to generate URL: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to generate URL: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestParam("objectName") String objectName) {
        
        try {
            storageService.deleteFile(objectName);
            return ResponseEntity.ok(ApiResponse.success(null, "File deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete file: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete file: " + e.getMessage()));
        }
    }
}
