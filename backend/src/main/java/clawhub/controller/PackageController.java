package clawhub.controller;

import clawhub.dto.*;
import clawhub.entity.PackageRelease;
import clawhub.service.PackageSecurityService;
import clawhub.service.PackageService;
import clawhub.service.PackageReleaseService;
import clawhub.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;
    private final PackageReleaseService packageReleaseService;
    private final PackageSecurityService packageSecurityService;
    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<PackageResponse> createPackage(
            @Valid @RequestBody PackageCreateRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        PackageResponse response = packageService.createPackage(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{name}")
    public ResponseEntity<PackageResponse> getPackage(@PathVariable String name) {
        PackageResponse response = packageService.getPackage(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PackageListResponse> listPackages(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String family,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Boolean official,
            @RequestParam(required = false) String runtimeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PackageListResponse response = packageService.listPackages(search, family, channel, official, runtimeId, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{name}")
    public ResponseEntity<PackageResponse> updatePackage(
            @PathVariable String name,
            @Valid @RequestBody PackageUpdateRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        PackageResponse response = packageService.updatePackage(name, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deletePackage(
            @PathVariable String name,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        packageService.deletePackage(name, userId);
        return ResponseEntity.noContent().build();
    }

    // Release endpoints
    @PostMapping("/{name}/versions")
    public ResponseEntity<PackageReleaseResponse> createRelease(
            @PathVariable String name,
            @Valid @RequestBody PackageReleaseRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        PackageReleaseResponse response = packageReleaseService.createRelease(name, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{name}/versions")
    public ResponseEntity<PackageReleaseListResponse> listReleases(
            @PathVariable String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PackageReleaseListResponse response = packageReleaseService.listReleases(name, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}/versions/{version}")
    public ResponseEntity<PackageReleaseResponse> getRelease(
            @PathVariable String name,
            @PathVariable String version) {
        PackageReleaseResponse response = packageReleaseService.getRelease(name, version);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{name}/versions/{version}")
    public ResponseEntity<Void> deleteRelease(
            @PathVariable String name,
            @PathVariable String version,
            @AuthenticationPrincipal OAuth2User principal) {
        UUID userId = extractUserId(principal);
        packageReleaseService.deleteRelease(name, version, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{name}/versions/latest")
    public ResponseEntity<PackageReleaseResponse> getLatestRelease(@PathVariable String name) {
        PackageReleaseResponse response = packageReleaseService.getLatestRelease(name);
        return ResponseEntity.ok(response);
    }

    /**
     * 下载包发布版本
     * 对应原版: packages.ts - 下载安全检查
     */
    @GetMapping("/{name}/versions/{version}/download")
    public ResponseEntity<?> downloadPackageRelease(
            @PathVariable String name,
            @PathVariable String version,
            @RequestParam(required = false) String file,
            @AuthenticationPrincipal OAuth2User principal) {

        // 获取发布版本
        PackageRelease release = packageReleaseService.findReleaseEntity(name, version);

        // 安全检查 - 使用新的 PackageSecurityService
        PackageSecurityService.DownloadSecurityBlock block =
            packageSecurityService.getPackageDownloadSecurityBlock(release);

        if (block != null) {
            return ResponseEntity.status(block.status())
                .body(ApiResponse.error(block.message()));
        }

        // 构建下载URL或返回文件
        // 这里简化处理，实际应该返回重定向或文件流
        String downloadUrl = storageService.getPackageDownloadUrl(
            release.getPackage_().getId(), release.getId());

        return ResponseEntity.ok(ApiResponse.success(
            Map.of("downloadUrl", downloadUrl),
            "Package download authorized"
        ));
    }

    private UUID extractUserId(OAuth2User principal) {
        if (principal == null) {
            throw new IllegalStateException("User not authenticated");
        }
        Map<String, Object> attributes = principal.getAttributes();
        Object userId = attributes.get("id");
        if (userId instanceof String) {
            return UUID.fromString((String) userId);
        }
        throw new IllegalStateException("Could not extract user ID from principal");
    }
}
