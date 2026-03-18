package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.Soul;
import clawhub.repository.SkillRepository;
import clawhub.repository.SoulRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SkillRepository skillRepository;
    private final SoulRepository soulRepository;

    /**
     * Search skills by text (name, summary, slug)
     * Basic implementation using database LIKE queries
     * Can be enhanced with Elasticsearch for full-text search
     */
    public Page<Skill> searchSkills(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return skillRepository.findAll(pageable);
        }
        
        // For now, use the existing repository methods
        // In Phase 4, this will be replaced with Elasticsearch
        String searchTerm = "%" + query.toLowerCase() + "%";
        
        // Get all skills and filter (inefficient but works for now)
        // This should be replaced with a proper search query
        List<Skill> allSkills = skillRepository.findAll();
        List<Skill> filtered = allSkills.stream()
                .filter(s -> s.getSlug().toLowerCase().contains(query.toLowerCase()) ||
                            (s.getDisplayName() != null && s.getDisplayName().toLowerCase().contains(query.toLowerCase())) ||
                            (s.getSummary() != null && s.getSummary().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        
        if (start > filtered.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, filtered.size());
        }
        
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    /**
     * Search souls by text
     */
    public Page<Soul> searchSouls(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return soulRepository.findAll(pageable);
        }
        
        List<Soul> allSouls = soulRepository.findAll();
        List<Soul> filtered = allSouls.stream()
                .filter(s -> s.getSlug().toLowerCase().contains(query.toLowerCase()) ||
                            (s.getDisplayName() != null && s.getDisplayName().toLowerCase().contains(query.toLowerCase())) ||
                            (s.getSummary() != null && s.getSummary().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        
        if (start > filtered.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, filtered.size());
        }
        
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    /**
     * Search both skills and souls
     */
    public SearchResult searchAll(String query, Pageable pageable) {
        Page<Skill> skills = searchSkills(query, pageable);
        Page<Soul> souls = searchSouls(query, pageable);
        
        return new SearchResult(skills, souls);
    }

    public record SearchResult(Page<Skill> skills, Page<Soul> souls) {}
}
