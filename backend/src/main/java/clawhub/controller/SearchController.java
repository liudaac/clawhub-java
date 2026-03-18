package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.dto.SkillResponse;
import clawhub.dto.SoulResponse;
import clawhub.entity.Skill;
import clawhub.entity.Soul;
import clawhub.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        Map<String, Object> result = new HashMap<>();
        
        if (type == null || type.equalsIgnoreCase("all")) {
            SearchService.SearchResult searchResult = searchService.searchAll(q, pageable);
            
            List<SkillResponse> skillResponses = searchResult.skills().getContent().stream()
                    .map(SkillResponse::fromEntityMinimal)
                    .collect(Collectors.toList());
            
            List<SoulResponse> soulResponses = searchResult.souls().getContent().stream()
                    .map(SoulResponse::fromEntityMinimal)
                    .collect(Collectors.toList());
            
            result.put("skills", skillResponses);
            result.put("souls", soulResponses);
            result.put("skillsTotal", searchResult.skills().getTotalElements());
            result.put("soulsTotal", searchResult.souls().getTotalElements());
            
        } else if (type.equalsIgnoreCase("skills")) {
            Page<Skill> skills = searchService.searchSkills(q, pageable);
            List<SkillResponse> skillResponses = skills.getContent().stream()
                    .map(SkillResponse::fromEntityMinimal)
                    .collect(Collectors.toList());
            
            result.put("skills", skillResponses);
            result.put("total", skills.getTotalElements());
            
        } else if (type.equalsIgnoreCase("souls")) {
            Page<Soul> souls = searchService.searchSouls(q, pageable);
            List<SoulResponse> soulResponses = souls.getContent().stream()
                    .map(SoulResponse::fromEntityMinimal)
                    .collect(Collectors.toList());
            
            result.put("souls", soulResponses);
            result.put("total", souls.getTotalElements());
        }
        
        result.put("query", q);
        result.put("page", page);
        result.put("size", size);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> searchSkills(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Skill> skills = searchService.searchSkills(q, pageable);
        
        List<SkillResponse> response = skills.getContent().stream()
                .map(SkillResponse::fromEntityMinimal)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, skills.getTotalElements()));
    }

    @GetMapping("/souls")
    public ResponseEntity<ApiResponse<List<SoulResponse>>> searchSouls(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Soul> souls = searchService.searchSouls(q, pageable);
        
        List<SoulResponse> response = souls.getContent().stream()
                .map(SoulResponse::fromEntityMinimal)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.paginated(response, page, size, souls.getTotalElements()));
    }
}
