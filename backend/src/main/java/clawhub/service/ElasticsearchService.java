package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.Soul;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    private static final String SKILLS_INDEX = "skills";
    private static final String SOULS_INDEX = "souls";

    public void indexSkill(Skill skill) {
        try {
            Map<String, Object> doc = Map.of(
                "id", skill.getId().toString(),
                "slug", skill.getSlug(),
                "displayName", skill.getDisplayName(),
                "summary", skill.getSummary() != null ? skill.getSummary() : "",
                "owner", skill.getOwner().getHandle(),
                "statsDownloads", skill.getStatsDownloads(),
                "statsStars", skill.getStatsStars(),
                "createdAt", skill.getCreatedAt().toString()
            );

            IndexRequest request = new IndexRequest(SKILLS_INDEX)
                .id(skill.getId().toString())
                .source(objectMapper.writeValueAsString(doc), XContentType.JSON);

            client.index(request, RequestOptions.DEFAULT);
            log.debug("Indexed skill: {}", skill.getSlug());
        } catch (IOException e) {
            log.error("Failed to index skill: {}", e.getMessage());
        }
    }

    public void indexSoul(Soul soul) {
        try {
            Map<String, Object> doc = Map.of(
                "id", soul.getId().toString(),
                "slug", soul.getSlug(),
                "displayName", soul.getDisplayName(),
                "summary", soul.getSummary() != null ? soul.getSummary() : "",
                "owner", soul.getOwner().getHandle(),
                "statsDownloads", soul.getStatsDownloads(),
                "statsStars", soul.getStatsStars(),
                "createdAt", soul.getCreatedAt().toString()
            );

            IndexRequest request = new IndexRequest(SOULS_INDEX)
                .id(soul.getId().toString())
                .source(objectMapper.writeValueAsString(doc), XContentType.JSON);

            client.index(request, RequestOptions.DEFAULT);
            log.debug("Indexed soul: {}", soul.getSlug());
        } catch (IOException e) {
            log.error("Failed to index soul: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> searchSkills(String query, int from, int size) {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery(query)
                    .field("displayName^3")
                    .field("slug^2")
                    .field("summary")
                    .field("owner")
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS))
                .from(from)
                .size(size);

            SearchRequest searchRequest = new SearchRequest(SKILLS_INDEX)
                .source(sourceBuilder);

            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            for (SearchHit hit : response.getHits().getHits()) {
                Map<String, Object> source = hit.getSourceAsMap();
                source.put("_score", hit.getScore());
                results.add(source);
            }

            log.debug("Search skills for '{}': {} results", query, results.size());
        } catch (IOException e) {
            log.error("Failed to search skills: {}", e.getMessage());
        }

        return results;
    }

    public List<Map<String, Object>> searchSouls(String query, int from, int size) {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery(query)
                    .field("displayName^3")
                    .field("slug^2")
                    .field("summary")
                    .field("owner")
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS))
                .from(from)
                .size(size);

            SearchRequest searchRequest = new SearchRequest(SOULS_INDEX)
                .source(sourceBuilder);

            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            for (SearchHit hit : response.getHits().getHits()) {
                Map<String, Object> source = hit.getSourceAsMap();
                source.put("_score", hit.getScore());
                results.add(source);
            }

            log.debug("Search souls for '{}': {} results", query, results.size());
        } catch (IOException e) {
            log.error("Failed to search souls: {}", e.getMessage());
        }

        return results;
    }
}
