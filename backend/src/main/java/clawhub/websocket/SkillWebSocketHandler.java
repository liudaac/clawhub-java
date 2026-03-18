package clawhub.websocket;

import clawhub.dto.SkillResponse;
import clawhub.entity.Skill;
import clawhub.service.SkillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final SkillService skillService;
    
    // Store all active sessions
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    
    // Store sessions subscribed to specific skills
    private final ConcurrentHashMap<String, Set<WebSocketSession>> skillSubscriptions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket connection established: {}", session.getId());
        
        // Send welcome message
        sendMessage(session, new WebSocketMessage("connected", "Connected to ClawHub real-time updates"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message: {}", payload);
        
        try {
            WebSocketCommand command = objectMapper.readValue(payload, WebSocketCommand.class);
            
            switch (command.getAction()) {
                case "subscribe":
                    handleSubscribe(session, command);
                    break;
                case "unsubscribe":
                    handleUnsubscribe(session, command);
                    break;
                case "ping":
                    sendMessage(session, new WebSocketMessage("pong", null));
                    break;
                default:
                    sendMessage(session, new WebSocketMessage("error", "Unknown action: " + command.getAction()));
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendMessage(session, new WebSocketMessage("error", e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        
        // Remove from all subscriptions
        skillSubscriptions.forEach((skillId, subs) -> subs.remove(session));
        
        log.info("WebSocket connection closed: {}", session.getId());
    }

    private void handleSubscribe(WebSocketSession session, WebSocketCommand command) throws IOException {
        String skillId = command.getSkillId();
        if (skillId == null) {
            sendMessage(session, new WebSocketMessage("error", "skillId is required"));
            return;
        }
        
        skillSubscriptions.computeIfAbsent(skillId, k -> ConcurrentHashMap.newKeySet()).add(session);
        
        // Send current state
        skillService.findById(java.util.UUID.fromString(skillId))
                .ifPresent(skill -> {
                    try {
                        sendMessage(session, new WebSocketMessage("skill_update", 
                                SkillResponse.fromEntity(skill)));
                    } catch (IOException e) {
                        log.error("Error sending skill update", e);
                    }
                });
        
        sendMessage(session, new WebSocketMessage("subscribed", "Subscribed to skill: " + skillId));
    }

    private void handleUnsubscribe(WebSocketSession session, WebSocketCommand command) throws IOException {
        String skillId = command.getSkillId();
        if (skillId != null) {
            Set<WebSocketSession> subs = skillSubscriptions.get(skillId);
            if (subs != null) {
                subs.remove(session);
            }
        }
        sendMessage(session, new WebSocketMessage("unsubscribed", "Unsubscribed"));
    }

    // Public method to broadcast skill updates
    public void broadcastSkillUpdate(Skill skill) {
        String skillId = skill.getId().toString();
        Set<WebSocketSession> subs = skillSubscriptions.get(skillId);
        
        if (subs != null && !subs.isEmpty()) {
            WebSocketMessage message = new WebSocketMessage("skill_update", 
                    SkillResponse.fromEntity(skill));
            
            subs.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        sendMessage(session, message);
                    }
                } catch (IOException e) {
                    log.error("Error broadcasting to session", e);
                }
            });
        }
    }

    // Public method to broadcast new skills
    public void broadcastNewSkill(Skill skill) {
        WebSocketMessage message = new WebSocketMessage("new_skill", 
                SkillResponse.fromEntityMinimal(skill));
        
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            } catch (IOException e) {
                log.error("Error broadcasting new skill", e);
            }
        });
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    // DTOs
    public static class WebSocketCommand {
        private String action;
        private String skillId;
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getSkillId() { return skillId; }
        public void setSkillId(String skillId) { this.skillId = skillId; }
    }

    public static class WebSocketMessage {
        private String type;
        private Object data;
        private long timestamp;
        
        public WebSocketMessage(String type, Object data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
