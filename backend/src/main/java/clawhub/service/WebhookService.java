package clawhub.service;

import clawhub.entity.Skill;
import clawhub.entity.SkillVersion;
import clawhub.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 技能发布事件
     */
    public void notifySkillPublished(Skill skill, SkillVersion version) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "skill.published");
        payload.put("timestamp", Instant.now().toString());
        payload.put("data", Map.of(
                "skillSlug", skill.getSlug(),
                "skillName", skill.getDisplayName(),
                "version", version.getVersion(),
                "owner", skill.getOwner().getHandle()
        ));

        // 这里应该从配置中读取 webhook URLs
        // 简化实现，实际应该支持多个订阅者
        log.debug("Skill published event: {}", skill.getSlug());
    }

    /**
     * 技能更新事件
     */
    public void notifySkillUpdated(Skill skill, SkillVersion version) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "skill.updated");
        payload.put("timestamp", Instant.now().toString());
        payload.put("data", Map.of(
                "skillSlug", skill.getSlug(),
                "skillName", skill.getDisplayName(),
                "version", version.getVersion()
        ));

        log.debug("Skill updated event: {}", skill.getSlug());
    }

    /**
     * 技能删除事件
     */
    public void notifySkillDeleted(Skill skill, User deletedBy) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "skill.deleted");
        payload.put("timestamp", Instant.now().toString());
        payload.put("data", Map.of(
                "skillSlug", skill.getSlug(),
                "deletedBy", deletedBy.getHandle()
        ));

        log.debug("Skill deleted event: {}", skill.getSlug());
    }

    /**
     * 所有权转移事件
     */
    public void notifySkillTransferred(Skill skill, User fromUser, User toUser) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "skill.transferred");
        payload.put("timestamp", Instant.now().toString());
        payload.put("data", Map.of(
                "skillSlug", skill.getSlug(),
                "fromUser", fromUser.getHandle(),
                "toUser", toUser.getHandle()
        ));

        log.debug("Skill transferred event: {} -> {}", fromUser.getHandle(), toUser.getHandle());
    }

    /**
     * 发送 webhook 到指定 URL
     */
    @CircuitBreaker(name = "webhook")
    public boolean sendWebhook(String url, String secret, Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            String signature = generateSignature(body, secret);

            var response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Webhook-Signature", signature)
                    .header("X-Webhook-Timestamp", String.valueOf(Instant.now().getEpochSecond()))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            log.debug("Webhook sent successfully to: {}", url);
            return true;
        } catch (Exception e) {
            log.error("Failed to send webhook to: {}", url, e);
            return false;
        }
    }

    /**
     * 生成 webhook 签名
     */
    private String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            log.error("Failed to generate webhook signature", e);
            return "";
        }
    }

    /**
     * 验证 webhook 签名
     */
    public boolean verifySignature(String payload, String signature, String secret) {
        try {
            String expected = generateSignature(payload, secret);
            return expected.equals(signature);
        } catch (Exception e) {
            log.error("Failed to verify webhook signature", e);
            return false;
        }
    }
}
