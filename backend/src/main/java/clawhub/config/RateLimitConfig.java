package clawhub.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    // Store buckets per IP address
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Bean
    public RateLimitService rateLimitService() {
        return new RateLimitService(buckets);
    }

    public static class RateLimitService {
        private final Map<String, Bucket> buckets;

        public RateLimitService(Map<String, Bucket> buckets) {
            this.buckets = buckets;
        }

        public Bucket resolveBucket(String key) {
            return buckets.computeIfAbsent(key, k -> createNewBucket());
        }

        private Bucket createNewBucket() {
            // 100 requests per minute
            Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }

        public Bucket resolveBucketForUser(String userId) {
            return buckets.computeIfAbsent("user:" + userId, k -> {
                // Authenticated users get higher limits: 1000 requests per minute
                Bandwidth limit = Bandwidth.classic(1000, Refill.intervally(1000, Duration.ofMinutes(1)));
                return Bucket.builder()
                        .addLimit(limit)
                        .build();
            });
        }
    }
}
