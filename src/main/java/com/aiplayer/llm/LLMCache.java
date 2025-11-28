package com.aiplayer.llm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * LLM Response Cache - Reduces API costs by caching responses.
 *
 * Important for AI players because:
 * - Similar situations occur frequently (e.g., "find food", "avoid creeper")
 * - API calls are expensive (GPT-4: ~$0.03/1k tokens)
 * - Deterministic responses (low temperature) are safe to cache
 *
 * Cache strategy:
 * - Key: hash of (prompt + options)
 * - Size: 1000 entries (~10MB memory)
 * - Expiration: 1 hour
 * - Eviction: LRU (least recently used)
 *
 * Implements LLMProvider to act as a transparent caching layer.
 */
public class LLMCache implements LLMProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMCache.class);

    private final Cache<String, String> cache;
    private final LLMProvider provider;
    private long hits = 0;
    private long misses = 0;

    public LLMCache(LLMProvider provider) {
        this(provider, 1000, 60);
    }

    public LLMCache(LLMProvider provider, int maxSize, int expirationMinutes) {
        this.provider = provider;
        this.cache = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(expirationMinutes, TimeUnit.MINUTES)
            .recordStats()
            .build();

        LOGGER.info("Initialized LLM cache: provider={}, maxSize={}, expiration={}min",
            provider.getProviderName(), maxSize, expirationMinutes);
    }

    /**
     * Complete with caching.
     */
    public CompletableFuture<String> complete(String prompt, LLMOptions options) {
        String cacheKey = buildCacheKey(prompt, options);

        // Check cache first
        String cachedResult = cache.getIfPresent(cacheKey);
        if (cachedResult != null) {
            hits++;
            if (hits % 10 == 0) {
                logStats();
            }
            return CompletableFuture.completedFuture(cachedResult);
        }

        // Cache miss - call provider
        misses++;
        return provider.complete(prompt, options)
            .thenApply(result -> {
                cache.put(cacheKey, result);
                return result;
            });
    }

    /**
     * Batch complete (no caching for batches - they're usually unique).
     */
    public CompletableFuture<List<String>> completeBatch(List<String> prompts, LLMOptions options) {
        return provider.completeBatch(prompts, options);
    }

    /**
     * Check if provider is available.
     */
    public boolean isAvailable() {
        return provider.isAvailable();
    }

    /**
     * Get model name.
     */
    public String getModelName() {
        return provider.getModelName();
    }

    /**
     * Get provider name.
     */
    public String getProviderName() {
        return provider.getProviderName();
    }

    /**
     * Get underlying provider (for advanced use).
     */
    public LLMProvider getProvider() {
        return provider;
    }

    /**
     * Clear cache.
     */
    public void clearCache() {
        cache.invalidateAll();
        LOGGER.info("Cleared LLM cache");
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        return new CacheStats(
            hits,
            misses,
            cache.estimatedSize(),
            getCacheHitRate()
        );
    }

    /**
     * Get cache hit rate (0.0 to 1.0).
     */
    public double getCacheHitRate() {
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    /**
     * Log cache statistics.
     */
    private void logStats() {
        long total = hits + misses;
        double hitRate = getCacheHitRate();
        LOGGER.info("LLM Cache Stats: hits={}, misses={}, hit_rate={:.1%}, size={}",
            hits, misses, hitRate, cache.estimatedSize());
    }

    /**
     * Build cache key from prompt and options.
     *
     * Key components:
     * - Prompt text
     * - System prompt
     * - Temperature (rounded to 1 decimal)
     * - Model name
     */
    private String buildCacheKey(String prompt, LLMOptions options) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(provider.getModelName()).append("|");

        if (options.getSystemPrompt() != null) {
            keyBuilder.append(options.getSystemPrompt()).append("|");
        }

        keyBuilder.append(prompt).append("|");
        keyBuilder.append(String.format("%.1f", options.getTemperature()));

        return keyBuilder.toString();
    }

    /**
     * Cache statistics.
     */
    public static class CacheStats {
        private final long hits;
        private final long misses;
        private final long size;
        private final double hitRate;

        public CacheStats(long hits, long misses, long size, double hitRate) {
            this.hits = hits;
            this.misses = misses;
            this.size = size;
            this.hitRate = hitRate;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public long getSize() {
            return size;
        }

        public double getHitRate() {
            return hitRate;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, size=%d, hitRate=%.1f%%}",
                hits, misses, size, hitRate * 100);
        }
    }
}
