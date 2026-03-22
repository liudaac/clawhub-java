package clawhub.service;

import clawhub.dto.SkillSearchRequest;
import clawhub.dto.SkillSearchResponse;
import clawhub.entity.Skill;
import clawhub.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillSearchService {

    private final SkillRepository skillRepository;
    private final ElasticsearchService elasticsearchService;

    /**
     * V4 搜索 API - 支持游标分页
     */
    @Transactional(readOnly = true)
    public SkillSearchResponse searchV4(SkillSearchRequest request) {
        // 解析游标
        Cursor cursor = parseCursor(request.getCursor());
        
        // 构建排序
        Sort sort = buildSort(request.getSortBy(), request.getSortDirection());
        Pageable pageable = PageRequest.of(cursor.getPage(), request.getSize(), sort);

        // 执行查询
        Page<Skill> skills;
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            // 使用 Elasticsearch 进行全文搜索
            skills = elasticsearchService.searchSkills(
                    request.getQuery(),
                    request.isNonSuspiciousOnly(),
                    pageable
            );
        } else {
            // 使用数据库查询
            skills = skillRepository.findPublicSkills(
                    request.isNonSuspiciousOnly(),
                    pageable
            );
        }

        // 构建响应
        List<SkillSearchResponse.SkillSearchItem> items = skills.getContent().stream()
                .map(this::toSearchItem)
                .collect(Collectors.toList());

        String nextCursor = skills.hasNext() 
                ? encodeCursor(cursor.getPage() + 1, request.getSize()) 
                : null;

        return SkillSearchResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(skills.hasNext())
                .total(skills.getTotalElements())
                .build();
    }

    /**
     * 获取热门技能
     */
    @Transactional(readOnly = true)
    public List<SkillSearchResponse.SkillSearchItem> getTrending(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "statsDownloads"));
        return skillRepository.findPublicSkills(true, pageable)
                .getContent()
                .stream()
                .map(this::toSearchItem)
                .collect(Collectors.toList());
    }

    /**
     * 获取精选技能
     */
    @Transactional(readOnly = true)
    public List<SkillSearchResponse.SkillSearchItem> getHighlighted(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return skillRepository.findHighlighted(pageable)
                .getContent()
                .stream()
                .map(this::toSearchItem)
                .collect(Collectors.toList());
    }

    private SkillSearchResponse.SkillSearchItem toSearchItem(Skill skill) {
        var latestVersion = skill.getLatestVersion();
        
        return SkillSearchResponse.SkillSearchItem.builder()
                .slug(skill.getSlug())
                .displayName(skill.getDisplayName())
                .summary(skill.getSummary())
                .ownerHandle(skill.getOwner() != null ? skill.getOwner().getHandle() : null)
                .ownerAvatarUrl(skill.getOwner() != null ? skill.getOwner().getAvatarUrl() : null)
                .latestVersion(latestVersion != null ? latestVersion.getVersion() : null)
                .versionCreatedAt(latestVersion != null ? latestVersion.getCreatedAt() : null)
                .changelog(latestVersion != null ? latestVersion.getChangelog() : null)
                .downloads(skill.getStatsDownloads())
                .stars(skill.getStatsStars())
                .tags(skill.getTags() != null ? skill.getTags().keySet().stream().toList() : List.of())
                .badges(skill.getBadges())
                .moderationVerdict(skill.getModerationVerdict())
                .highlighted(isHighlighted(skill))
                .createdAt(skill.getCreatedAt())
                .updatedAt(skill.getUpdatedAt())
                .build();
    }

    private boolean isHighlighted(Skill skill) {
        // 根据下载量、星标数等判断是否为精选
        return skill.getStatsDownloads() > 100 || skill.getStatsStars() > 10;
    }

    private Sort buildSort(String sortBy, String direction) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        return switch (sortBy.toLowerCase()) {
            case "downloads" -> Sort.by(dir, "statsDownloads");
            case "stars" -> Sort.by(dir, "statsStars");
            case "updated" -> Sort.by(dir, "updatedAt");
            case "name" -> Sort.by(dir, "displayName");
            default -> Sort.by(dir, "createdAt");
        };
    }

    private Cursor parseCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return new Cursor(0, 20);
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            String[] parts = decoded.split(":");
            return new Cursor(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (Exception e) {
            return new Cursor(0, 20);
        }
    }

    private String encodeCursor(int page, int size) {
        String raw = page + ":" + size;
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    private record Cursor(int page, int size) {}
}
