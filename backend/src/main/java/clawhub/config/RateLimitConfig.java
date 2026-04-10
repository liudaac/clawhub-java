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

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter(buckets);
    }

    public static class RateLimiter {
        private final Map<String, Bucket> buckets;

        public RateLimiter(Map<String, Bucket> buckets) {
            this.buckets = buckets;
        }

        public Bucket resolveBucket(String key) {
            return buckets.computeIfAbsent(key, k -> createNewBucket());
        }

        private Bucket createNewBucket() {
            // 100 requests per minute per IP
            Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }

        public boolean tryConsume(String key, long tokens) {
            return resolveBucket(key).tryConsume(tokens);
        }
    }
}
