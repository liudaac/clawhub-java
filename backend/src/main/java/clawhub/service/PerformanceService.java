package clawhub.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    /**
     * 记录 API 调用时间
     */
    public <T> T recordApiCall(String apiName, Supplier<T> operation) {
        Timer timer = timers.computeIfAbsent("api." + apiName, 
                k -> Timer.builder(k)
                        .description("API call duration")
                        .register(meterRegistry));
        
        return timer.record(operation);
    }

    /**
     * 记录数据库查询时间
     */
    public <T> T recordDbQuery(String queryName, Supplier<T> operation) {
        Timer timer = timers.computeIfAbsent("db." + queryName,
                k -> Timer.builder(k)
                        .description("Database query duration")
                        .register(meterRegistry));
        
        return timer.record(operation);
    }

    /**
     * 记录缓存操作
     */
    public void recordCacheHit(String cacheName) {
        Counter counter = counters.computeIfAbsent("cache." + cacheName + ".hits",
                k -> Counter.builder(k)
                        .description("Cache hits")
                        .register(meterRegistry));
        counter.increment();
    }

    public void recordCacheMiss(String cacheName) {
        Counter counter = counters.computeIfAbsent("cache." + cacheName + ".misses",
                k -> Counter.builder(k)
                        .description("Cache misses")
                        .register(meterRegistry));
        counter.increment();
    }

    /**
     * 记录搜索操作
     */
    public <T> T recordSearch(String searchType, Supplier<T> operation) {
        Timer timer = timers.computeIfAbsent("search." + searchType,
                k -> Timer.builder(k)
                        .description("Search operation duration")
                        .register(meterRegistry));
        
        Counter counter = counters.computeIfAbsent("search." + searchType + ".count",
                k -> Counter.builder(k)
                        .description("Search operation count")
                        .register(meterRegistry));
        counter.increment();
        
        return timer.record(operation);
    }

    /**
     * 记录安全扫描
     */
    public void recordSecurityScan(String scanType, long durationMs, boolean success) {
        Timer timer = timers.computeIfAbsent("security.scan." + scanType,
                k -> Timer.builder(k)
                        .description("Security scan duration")
                        .register(meterRegistry));
        timer.record(durationMs, TimeUnit.MILLISECONDS);
        
        Counter counter = counters.computeIfAbsent("security.scan." + scanType + (success ? ".success" : ".failure"),
                k -> Counter.builder(k)
                        .description("Security scan results")
                        .register(meterRegistry));
        counter.increment();
    }

    /**
     * 记录 Package 操作
     */
    public void recordPackageOperation(String operation) {
        Counter counter = counters.computeIfAbsent("package." + operation,
                k -> Counter.builder(k)
                        .description("Package operations")
                        .register(meterRegistry));
        counter.increment();
    }

    /**
     * 记录下载
     */
    public void recordDownload(String packageName, long bytes) {
        Counter counter = counters.computeIfAbsent("package.downloads",
                k -> Counter.builder(k)
                        .description("Package downloads")
                        .register(meterRegistry));
        counter.increment();
        
        Counter bytesCounter = counters.computeIfAbsent("package.download.bytes",
                k -> Counter.builder(k)
                        .description("Download bytes")
                        .register(meterRegistry));
        bytesCounter.increment(bytes);
    }

    /**
     * 获取性能指标
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        
        // Timer metrics
        timers.forEach((name, timer) -> {
            Map<String, Object> timerMetrics = new ConcurrentHashMap<>();
            timerMetrics.put("count", timer.count());
            timerMetrics.put("mean", timer.mean(TimeUnit.MILLISECONDS));
            timerMetrics.put("max", timer.max(TimeUnit.MILLISECONDS));
            timerMetrics.put("totalTime", timer.totalTime(TimeUnit.MILLISECONDS));
            metrics.put(name, timerMetrics);
        });
        
        // Counter metrics
        counters.forEach((name, counter) -> {
            metrics.put(name, counter.count());
        });
        
        return metrics;
    }

    /**
     * 简单的计时器工具
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample, String timerName) {
        Timer timer = timers.computeIfAbsent(timerName,
                k -> Timer.builder(k).register(meterRegistry));
        sample.stop(timer);
    }
}
