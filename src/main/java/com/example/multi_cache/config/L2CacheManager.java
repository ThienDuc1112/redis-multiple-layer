package com.example.multi_cache.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class L2CacheManager {

    private static final String PRODUCT_KEY_PREFIX = "product_";

    /**
     * Đồng bộ với RedisCacheConfiguration
     * products TTL = 5 phút
     */
    private static final Duration PRODUCT_TTL = Duration.ofMinutes(5);

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public L2CacheManager(CacheManager cacheManager,
                          RedisTemplate<String, Object> redisTemplate) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
        log.info("L2 Cache Manager initialized with Redis");
    }

    /**
     * Put product vào Redis
     */
    public void putProduct(Long productId, Object product) {
        String key = buildCacheKey(productId);

        redisTemplate.opsForValue().set(
                key,
                product,
                PRODUCT_TTL
        );

        log.debug("Put product {} into L2 cache", productId);
    }

    /**
     * Get product từ Redis
     */
    public Object getProduct(Long productId) {
        String key = buildCacheKey(productId);

        Object value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            log.debug("L2 cache hit for product {}", productId);
        } else {
            log.debug("L2 cache miss for product {}", productId);
        }

        return value;
    }

    /**
     * Evict 1 product
     */
    public void evictProduct(Long productId) {
        String key = buildCacheKey(productId);

        redisTemplate.delete(key);

        log.debug("Evicted product {} from L2 cache", productId);
    }

    /**
     * Evict toàn bộ products
     */
    public void evictAllProducts() {
        Set<String> keys = redisTemplate.keys(PRODUCT_KEY_PREFIX + "*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        log.info("Evicted all products from L2 cache");
    }


    /**
     * Kiểm tra key tồn tại
     */
    public boolean isProductCached(Long productId) {

        Boolean exists =
                redisTemplate.hasKey(buildCacheKey(productId));

        return Boolean.TRUE.equals(exists);
    }

    /**
     * Gia hạn TTL
     */
    public boolean refreshTTL(
            Long productId,
            long timeout,
            TimeUnit unit
    ) {

        Boolean result = redisTemplate.expire(
                buildCacheKey(productId),
                timeout,
                unit
        );

        return Boolean.TRUE.equals(result);
    }

    private String buildCacheKey(Long productId) {
        return PRODUCT_KEY_PREFIX + productId;
    }
}
