package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.dto.SkillSearchRequest;
import clawhub.dto.SkillSearchResponse;
import clawhub.service.SkillSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
@Tag(name = "Skill Search V4", description = "V4 搜索 API")
public class SkillSearchController {

    private final SkillSearchService searchService;

    @GetMapping
    @Operation(summary = "V4 搜索技能", description = "支持游标分页的搜索 API")
    public ResponseEntity<ApiResponse<SkillSearchResponse>> searchSkills(
            @Valid @ModelAttribute SkillSearchRequest request) {
        SkillSearchResponse response = searchService.searchV4(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/trending")
    @Operation(summary = "获取热门技能")
    public ResponseEntity<ApiResponse<List<SkillSearchResponse.SkillSearchItem>>> getTrending(
            @RequestParam(defaultValue = "10") int limit) {
        List<SkillSearchResponse.SkillSearchItem> items = searchService.getTrending(limit);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/highlighted")
    @Operation(summary = "获取精选技能")
    public ResponseEntity<ApiResponse<List<SkillSearchResponse.SkillSearchItem>>> getHighlighted(
            @RequestParam(defaultValue = "10") int limit) {
        List<SkillSearchResponse.SkillSearchItem> items = searchService.getHighlighted(limit);
        return ResponseEntity.ok(ApiResponse.success(items));
    }
}
