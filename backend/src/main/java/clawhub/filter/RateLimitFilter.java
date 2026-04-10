package clawhub.filter;

import clawhub.config.RateLimitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class RateLimitFilter implements Filter {

    private final RateLimitConfig.RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitConfig.RateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);
        String path = httpRequest.getRequestURI();

        // Skip rate limiting for health checks and static resources
        if (path.startsWith("/actuator") || path.startsWith("/static") || path.startsWith("/public")) {
            chain.doFilter(request, response);
            return;
        }

        String key = clientIp + ":" + path;

        if (!rateLimiter.tryConsume(key, 1)) {
            log.warn("Rate limit exceeded for IP: {}, path: {}", clientIp, path);

            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", "Rate limit exceeded",
                    "message", "Too many requests. Please try again later.",
                    "retryAfter", 60
            );

            objectMapper.writeValue(httpResponse.getOutputStream(), errorResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
