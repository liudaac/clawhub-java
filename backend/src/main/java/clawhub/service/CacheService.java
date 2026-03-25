package clawhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 清除指定缓存
     */
    public void evictCache(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Evicted cache: {} - {}", cacheName, key);
        }
    }

    /**
     * 清除整个缓存
     */
    public void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cleared cache: {}", cacheName);
        }
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(this::clearCache);
        log.info("Cleared all caches");
    }

    /**
     * 设置缓存值（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存值
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     */
    public void deletePattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Deleted {} keys matching pattern: {}", keys.size(), pattern);
        }
    }

    /**
     * 检查键是否存在
     */
    public boolean hasKey(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    /**
     * 清除 Package 相关缓存
     */
    public void evictPackageCaches(String packageName) {
        evictCache("packages", packageName);
        clearCache("packageLists");
        deletePattern("packages::" + packageName + "*");
        log.debug("Evicted package caches for: {}", packageName);
    }

    /**
     * 清除 Publisher 相关缓存
     */
    public void evictPublisherCaches(String handle) {
        evictCache("publishers", handle);
        clearCache("packageLists");
        log.debug("Evicted publisher caches for: {}", handle);
    }

    /**
     * 清除 Skill 相关缓存
     */
    public void evictSkillCaches(String slug) {
        evictCache("skills", slug);
        clearCache("searchResults");
        log.debug("Evicted skill caches for: {}", slug);
    }
}
