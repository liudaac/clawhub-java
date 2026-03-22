package clawhub.service;

import clawhub.entity.SkillVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmSecurityService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1}")
    private String apiUrl;

    @Value("${openai.model:gpt-5-mini}")
    private String model;

    @Value("${llm.security.enabled:false}")
    private boolean enabled;

    private static final int MAX_OUTPUT_TOKENS = 16000;

    private WebClient getClient() {
        return webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 对技能进行安全评估
     */
    @CircuitBreaker(name = "openai", fallbackMethod = "evaluateFallback")
    @Retry(name = "openai")
    public Optional<SkillVersion.LlmSecurityAnalysis> evaluateSkill(SkillEvalContext context) {
        if (!enabled || apiKey.isEmpty()) {
            log.debug("LLM security evaluation is disabled or API key not configured");
            return Optional.empty();
        }

        try {
            log.info("Evaluating skill security: {} (v{})", context.slug(), context.version());

            String systemPrompt = SECURITY_EVALUATOR_SYSTEM_PROMPT;
            String userPrompt = buildEvalPrompt(context);

            Map<String, Object> request = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "max_tokens", MAX_OUTPUT_TOKENS,
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object")
            );

            var response = getClient().post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(120))
                    .block();

            if (response == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            return parseEvalResponse(content);

        } catch (Exception e) {
            log.error("Failed to evaluate skill security", e);
            throw e;
        }
    }

    private String buildEvalPrompt(SkillEvalContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Evaluate this OpenClaw skill for security concerns.\n\n");
        prompt.append("Skill: ").append(context.slug()).append("\n");
        prompt.append("Display Name: ").append(context.displayName()).append("\n");
        prompt.append("Version: ").append(context.version()).append("\n");
        prompt.append("Summary: ").append(context.summary() != null ? context.summary() : "N/A").append("\n\n");

        if (context.parsedMetadata() != null) {
            prompt.append("Metadata:\n");
            context.parsedMetadata().forEach((key, value) -> {
                prompt.append("  ").append(key).append(": ").append(value).append("\n");
            });
            prompt.append("\n");
        }

        prompt.append("Files:\n");
        for (SkillVersion.FileInfo file : context.files()) {
            prompt.append("  - ").append(file.getPath()).append(" (").append(file.getSize()).append(" bytes)\n");
        }
        prompt.append("\n");

        if (context.skillMdContent() != null && !context.skillMdContent().isEmpty()) {
            prompt.append("SKILL.md content:\n");
            prompt.append(truncate(context.skillMdContent(), 8000)).append("\n\n");
        }

        if (context.injectionSignals() != null && !context.injectionSignals().isEmpty()) {
            prompt.append("Static scan findings:\n");
            for (String signal : context.injectionSignals()) {
                prompt.append("  - ").append(signal).append("\n");
            }
        }

        return prompt.toString();
    }

    private Optional<SkillVersion.LlmSecurityAnalysis> parseEvalResponse(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);

            SkillVersion.LlmSecurityAnalysis analysis = SkillVersion.LlmSecurityAnalysis.builder()
                    .verdict(root.path("verdict").asText("benign"))
                    .confidence(root.path("confidence").asText("low"))
                    .summary(root.path("summary").asText(""))
                    .guidance(root.path("guidance").asText(""))
                    .analyzedAt(Instant.now())
                    .model(model)
                    .dimensions(parseDimensions(root.path("dimensions")))
                    .findings(parseFindings(root.path("findings")))
                    .build();

            return Optional.of(analysis);
        } catch (Exception e) {
            log.error("Failed to parse LLM eval response", e);
            return Optional.empty();
        }
    }

    private List<SkillVersion.LlmSecurityAnalysis.Dimension> parseDimensions(JsonNode dimensionsNode) {
        List<SkillVersion.LlmSecurityAnalysis.Dimension> dimensions = new ArrayList<>();
        if (dimensionsNode.isArray()) {
            for (JsonNode dim : dimensionsNode) {
                dimensions.add(SkillVersion.LlmSecurityAnalysis.Dimension.builder()
                        .name(dim.path("name").asText())
                        .label(dim.path("label").asText())
                        .rating(dim.path("rating").asText())
                        .detail(dim.path("detail").asText())
                        .build());
            }
        }
        return dimensions;
    }

    private List<SkillVersion.LlmSecurityAnalysis.Finding> parseFindings(JsonNode findingsNode) {
        List<SkillVersion.LlmSecurityAnalysis.Finding> findings = new ArrayList<>();
        if (findingsNode.isArray()) {
            for (JsonNode finding : findingsNode) {
                findings.add(SkillVersion.LlmSecurityAnalysis.Finding.builder()
                        .code(finding.path("code").asText())
                        .severity(finding.path("severity").asText("info"))
                        .file(finding.path("file").asText(""))
                        .line(finding.path("line").asInt(1))
                        .message(finding.path("message").asText(""))
                        .evidence(finding.path("evidence").asText(""))
                        .build());
            }
        }
        return findings;
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    // Fallback
    private Optional<SkillVersion.LlmSecurityAnalysis> evaluateFallback(SkillEvalContext context, Exception ex) {
        log.warn("LLM security evaluation fallback triggered: {}", ex.getMessage());
        return Optional.empty();
    }

    // Record for evaluation context
    public record SkillEvalContext(
            String slug,
            String displayName,
            String ownerUserId,
            String version,
            Instant createdAt,
            String summary,
            String source,
            String homepage,
            Map<String, Object> parsedMetadata,
            List<SkillVersion.FileInfo> files,
            String skillMdContent,
            List<String> injectionSignals
    ) {}

    // System prompt for security evaluation
    private static final String SECURITY_EVALUATOR_SYSTEM_PROMPT = """
You are a security evaluator for OpenClaw AI skills. Users install skills to extend what their AI agent can do.

You are not a malware classifier. You are an incoherence detector.

A skill is a bundle of: a name, a description, a set of instructions (SKILL.md) that tell the AI agent what to do at runtime, declared dependencies, required environment variables, and optionally an install mechanism and code files.

## How to evaluate

Rate each dimension from 1-5 and provide detailed findings.

Dimensions to evaluate:
1. **name_accuracy** - Does the name match what the skill actually does?
2. **description_honesty** - Does the description accurately describe capabilities?
3. **permission_proportion** - Are requested permissions appropriate for the functionality?
4. **dependency_appropriateness** - Are dependencies justified and from trusted sources?
5. **install_safety** - Does the install process follow safe practices?

## Response format

Return JSON with this structure:
{
  "verdict": "benign|suspicious|malicious",
  "confidence": "high|medium|low",
  "summary": "Brief summary of findings",
  "guidance": "What users should know",
  "dimensions": [
    {"name": "name_accuracy", "label": "Name Accuracy", "rating": "1-5", "detail": "..."}
  ],
  "findings": [
    {"code": "FINDING_CODE", "severity": "info|warn|critical", "file": "...", "line": 1, "message": "...", "evidence": "..."}
  ]
}

Be thorough but concise. Focus on actual security concerns, not style preferences.
""";
}