package clawhub.controller;

import clawhub.dto.ApiResponse;
import clawhub.dto.UserResponse;
import clawhub.entity.User;
import clawhub.security.CurrentUser;
import clawhub.security.JwtService;
import clawhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping("/whoami")
    public ResponseEntity<ApiResponse<UserResponse>> whoami(@CurrentUser User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromEntity(currentUser)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        // JWT tokens are stateless, so we can't really invalidate them server-side
        // Client should remove the token from storage
        log.info("User logged out");
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/token/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(@CurrentUser User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error("Not authenticated"));
        }

        String newToken = jwtService.generateToken(currentUser.getId(), currentUser.getHandle(), 
                currentUser.getRole().name());

        Map<String, String> response = new HashMap<>();
        response.put("token", newToken);
        response.put("expiresIn", String.valueOf(jwtService.getExpirationTime()));

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
