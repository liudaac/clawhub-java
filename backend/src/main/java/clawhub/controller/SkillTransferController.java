package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.entity.SkillTransfer;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
import clawhub.service.SkillTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/skills/{skillSlug}/transfers")
@RequiredArgsConstructor
@Tag(name = "Skill Transfers", description = "技能所有权转移管理")
@SecurityRequirement(name = "bearerAuth")
public class SkillTransferController {

    private final SkillTransferService transferService;

    @GetMapping
    @Operation(summary = "获取技能的转移请求列表")
    public ResponseEntity<ApiResponse<Page<SkillTransfer>>> listTransfers(
            @PathVariable String skillSlug,
            @PageableDefault(size = 20) Pageable pageable) {
        // 这里需要添加权限检查
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "请求技能所有权转移")
    public ResponseEntity<ApiResponse<SkillTransfer>> requestTransfer(
            @PathVariable String skillSlug,
            @Valid @RequestBody TransferRequest request,
            @CurrentUser User currentUser) {
        SkillTransfer transfer = transferService.requestTransfer(
                skillSlug, request.toUserHandle(), request.message(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(transfer));
    }

    @PostMapping("/{transferId}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "接受转移请求")
    public ResponseEntity<ApiResponse<SkillTransfer>> acceptTransfer(
            @PathVariable String skillSlug,
            @PathVariable UUID transferId,
            @RequestBody(required = false) TransferResponse request,
            @CurrentUser User currentUser) {
        String message = request != null ? request.message() : null;
        SkillTransfer transfer = transferService.acceptTransfer(transferId, message, currentUser);
        return ResponseEntity.ok(ApiResponse.success(transfer));
    }

    @PostMapping("/{transferId}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "拒绝转移请求")
    public ResponseEntity<ApiResponse<SkillTransfer>> rejectTransfer(
            @PathVariable String skillSlug,
            @PathVariable UUID transferId,
            @RequestBody(required = false) TransferResponse request,
            @CurrentUser User currentUser) {
        String message = request != null ? request.message() : null;
        SkillTransfer transfer = transferService.rejectTransfer(transferId, message, currentUser);
        return ResponseEntity.ok(ApiResponse.success(transfer));
    }

    @DeleteMapping("/{transferId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "取消转移请求")
    public ResponseEntity<ApiResponse<SkillTransfer>> cancelTransfer(
            @PathVariable String skillSlug,
            @PathVariable UUID transferId,
            @CurrentUser User currentUser) {
        SkillTransfer transfer = transferService.cancelTransfer(transferId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(transfer));
    }

    @GetMapping("/incoming")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取传入的转移请求")
    public ResponseEntity<ApiResponse<Page<SkillTransfer>>> getIncomingTransfers(
            @CurrentUser User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SkillTransfer> transfers = transferService.listIncoming(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(transfers));
    }

    @GetMapping("/outgoing")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取传出的转移请求")
    public ResponseEntity<ApiResponse<Page<SkillTransfer>>> getOutgoingTransfers(
            @CurrentUser User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SkillTransfer> transfers = transferService.listOutgoing(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(transfers));
    }

    // Request/Response records
    public record TransferRequest(
            @NotBlank String toUserHandle,
            String message
    ) {}

    public record TransferResponse(
            String message
    ) {}
}
