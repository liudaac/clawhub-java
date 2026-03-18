package clawhub.security;

import clawhub.entity.User;
import clawhub.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService userService;

    @Value("${app.oauth2.redirect-uri:http://localhost:5173/auth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();
        
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // Extract GitHub user info
        Long githubId = Long.valueOf(attributes.get("id").toString());
        String login = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("avatar_url");
        String bio = (String) attributes.get("bio");
        String createdAt = (String) attributes.get("created_at");
        
        // Create or update user
        User user = userService.findOrCreateUser(githubId, login, name, avatarUrl, bio, 
                createdAt != null ? Instant.parse(createdAt) : null);
        
        // Generate JWT token
        String token = jwtService.generateToken(user.getId(), user.getHandle(), user.getRole().name());
        
        // Redirect to frontend with token
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .queryParam("userId", user.getId())
                .queryParam("handle", user.getHandle())
                .build().toUriString();
        
        log.info("OAuth2 login successful for user: {}", user.getHandle());
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
