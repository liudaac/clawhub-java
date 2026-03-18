package clawhub.filter;

import clawhub.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final RateLimitConfig.RateLimitService rateLimitService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Skip rate limiting for certain paths
        String path = httpRequest.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/ws")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Get client identifier (IP or user ID)
        String clientId = getClientIdentifier(httpRequest);
        Bucket bucket;
        
        // Check if user is authenticated
        if (httpRequest.getUserPrincipal() != null) {
            bucket = rateLimitService.resolveBucketForUser(httpRequest.getUserPrincipal().getName());
        } else {
            bucket = rateLimitService.resolveBucket(clientId);
        }
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Add rate limit headers
            httpResponse.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            httpResponse.addHeader("X-RateLimit-Retry-After", String.valueOf(waitForRefill));
            httpResponse.setStatus(429);
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Please try again later.\"}");
            log.warn("Rate limit exceeded for client: {}", clientId);
        }
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            // Take the first IP if multiple are present
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
