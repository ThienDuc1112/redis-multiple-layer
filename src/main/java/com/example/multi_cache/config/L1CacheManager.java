package com.example.multi_cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

//@Component
@Slf4j
public class L1CacheManager {

    // Cache Caffeine cho products
    private final Cache<String, Object> productCache;

    // Cache Manager để thao tác với Spring Cache abstraction
    private final CacheManager cacheManager;

    public L1CacheManager(
            @Qualifier("caffeineCacheManager")
            CacheManager cacheManager) {

        this.cacheManager = cacheManager;

        this.productCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
    }

    /**
     * Khởi tạo và kiểm tra cache
     */
    @PostConstruct
    public void init() {
        log.info("L1 Cache Manager initialized successfully. Cache stats: {}",
                productCache.stats());
    }

    /**
     * Xóa product khỏi L1 cache
     */
    public void evictProduct(Long productId) {
        String cacheKey = buildCacheKey(productId);
        productCache.invalidate(cacheKey);

        // Cũng invalidate qua Spring Cache abstraction
        org.springframework.cache.Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.evict(cacheKey);
        }

        log.debug("Evicted product {} from L1 cache", productId);
    }

    /**
     * Xóa tất cả products khỏi L1 cache
     */
    public void evictAllProducts() {
        productCache.invalidateAll();

        // Cũng clear qua Spring Cache abstraction
        org.springframework.cache.Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.clear();
        }

        log.info("Evicted all products from L1 cache");
    }

    /**
     * Thêm product vào L1 cache
     */
    public void putProduct(Long productId, Object product) {
        String cacheKey = buildCacheKey(productId);
        productCache.put(cacheKey, product);

        // Cũng put qua Spring Cache abstraction
        org.springframework.cache.Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.put(cacheKey, product);
        }

        log.debug("Added product {} to L1 cache", productId);
    }

    /**
     * Lấy product từ L1 cache
     */
    public Object getProduct(Long productId) {
        String cacheKey = buildCacheKey(productId);
        Object product = productCache.getIfPresent(cacheKey);

        if (product != null) {
            log.debug("Cache hit for product {} in L1", productId);
        } else {
            log.debug("Cache miss for product {} in L1", productId);
        }

        return product;
    }

    /**
     * Kiểm tra product có trong L1 cache không
     */
    public boolean isProductCached(Long productId) {
        String cacheKey = buildCacheKey(productId);
        return productCache.getIfPresent(cacheKey) != null;
    }

    /**
     * Lấy số lượng items trong L1 cache
     */
    public long getCacheSize() {
        return productCache.estimatedSize();
    }

    /**
     * Lấy cache stats
     */
    public String getCacheStats() {
        return productCache.stats().toString();
    }

    /**
     * Build cache key
     */
    private String buildCacheKey(Long productId) {
        return "product_" + productId;
    }

    /**
     * Lấy cache stats dưới dạng đọc được
     */
    public CacheStats getCacheStatsObject() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = productCache.stats();
        return CacheStats.builder()
                .hitCount(stats.hitCount())
                .missCount(stats.missCount())
                .hitRate(stats.hitRate())
                .missRate(stats.missRate())
                .loadCount(stats.loadCount())
                .evictionCount(stats.evictionCount())
                .totalLoadTime(stats.totalLoadTime())
                .build();
    }

    /**
     * Reset cache stats
     */
    public void resetCacheStats() {
        productCache.stats().toString(); // Caffeine không hỗ trợ reset stats
        log.info("Cache stats cannot be reset. Stats: {}", productCache.stats());
    }

    /**
     * Cache stats DTO
     */
    @lombok.Value
    @lombok.Builder
    public static class CacheStats {
        long hitCount;
        long missCount;
        double hitRate;
        double missRate;
        long loadCount;
        long evictionCount;
        long totalLoadTime;
    }
}
