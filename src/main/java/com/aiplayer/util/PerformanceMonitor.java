package com.aiplayer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring utility (Phase 6).
 *
 * Tracks:
 * - Operation execution times
 * - Call counts
 * - Memory usage
 * - Cache hit rates
 *
 * Phase 6: Optimization & Polish
 */
public class PerformanceMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMonitor.class);

    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();

    private final Map<String, OperationStats> operationStats;
    private final AtomicLong totalMemoryUsageBytes;

    private static class OperationStats {
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicLong totalTimeMs = new AtomicLong(0);
        private final AtomicLong maxTimeMs = new AtomicLong(0);
        private long lastCallTime = 0;

        public void record(long durationMs) {
            callCount.incrementAndGet();
            totalTimeMs.addAndGet(durationMs);

            // Update max
            long currentMax = maxTimeMs.get();
            while (durationMs > currentMax) {
                if (maxTimeMs.compareAndSet(currentMax, durationMs)) {
                    break;
                }
                currentMax = maxTimeMs.get();
            }

            lastCallTime = System.currentTimeMillis();
        }

        public long getCallCount() {
            return callCount.get();
        }

        public double getAverageTimeMs() {
            long count = callCount.get();
            return count > 0 ? (double) totalTimeMs.get() / count : 0;
        }

        public long getMaxTimeMs() {
            return maxTimeMs.get();
        }

        public long getTotalTimeMs() {
            return totalTimeMs.get();
        }

        public long getLastCallTime() {
            return lastCallTime;
        }
    }

    private PerformanceMonitor() {
        this.operationStats = new ConcurrentHashMap<>();
        this.totalMemoryUsageBytes = new AtomicLong(0);
    }

    public static PerformanceMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Start timing an operation.
     *
     * @param operationName The operation name
     * @return Start time in nanoseconds
     */
    public long startOperation(String operationName) {
        return System.nanoTime();
    }

    /**
     * End timing an operation and record stats.
     *
     * @param operationName The operation name
     * @param startTimeNanos Start time from startOperation()
     */
    public void endOperation(String operationName, long startTimeNanos) {
        long durationNanos = System.nanoTime() - startTimeNanos;
        long durationMs = durationNanos / 1_000_000;

        OperationStats stats = operationStats.computeIfAbsent(operationName, k -> new OperationStats());
        stats.record(durationMs);

        // Log slow operations (>100ms)
        if (durationMs > 100) {
            LOGGER.warn("Slow operation detected: {} took {}ms", operationName, durationMs);
        }
    }

    /**
     * Time an operation (convenience method).
     *
     * @param operationName The operation name
     * @param operation The operation to time
     */
    public void time(String operationName, Runnable operation) {
        long start = startOperation(operationName);
        try {
            operation.run();
        } finally {
            endOperation(operationName, start);
        }
    }

    /**
     * Get operation statistics.
     *
     * @param operationName The operation name
     * @return Statistics string
     */
    public String getOperationStats(String operationName) {
        OperationStats stats = operationStats.get(operationName);
        if (stats == null) {
            return operationName + ": no data";
        }

        return String.format("%s: count=%d, avg=%.2fms, max=%dms, total=%dms",
            operationName,
            stats.getCallCount(),
            stats.getAverageTimeMs(),
            stats.getMaxTimeMs(),
            stats.getTotalTimeMs());
    }

    /**
     * Get all performance statistics.
     *
     * @return Statistics report
     */
    public String getAllStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Performance Statistics ===\n");

        operationStats.entrySet().stream()
            .sorted((a, b) -> Long.compare(
                b.getValue().getTotalTimeMs(),
                a.getValue().getTotalTimeMs()
            ))
            .forEach(entry -> {
                sb.append(getOperationStats(entry.getKey())).append("\n");
            });

        // Memory stats
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemoryMB = runtime.maxMemory() / 1024 / 1024;

        sb.append(String.format("\nMemory: %dMB / %dMB", usedMemoryMB, maxMemoryMB));

        return sb.toString();
    }

    /**
     * Log statistics.
     */
    public void logStats() {
        LOGGER.info("\n{}", getAllStats());
    }

    /**
     * Reset all statistics.
     */
    public void reset() {
        operationStats.clear();
        LOGGER.info("Performance statistics reset");
    }

    /**
     * Get slow operations (avg time > threshold).
     *
     * @param thresholdMs Threshold in milliseconds
     * @return Map of operation names to average times
     */
    public Map<String, Double> getSlowOperations(long thresholdMs) {
        Map<String, Double> slowOps = new ConcurrentHashMap<>();

        operationStats.forEach((name, stats) -> {
            double avgTime = stats.getAverageTimeMs();
            if (avgTime > thresholdMs) {
                slowOps.put(name, avgTime);
            }
        });

        return slowOps;
    }

    /**
     * Get memory usage in MB.
     *
     * @return Current memory usage
     */
    public long getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }

    /**
     * Get max memory in MB.
     *
     * @return Max available memory
     */
    public long getMaxMemoryMB() {
        return Runtime.getRuntime().maxMemory() / 1024 / 1024;
    }

    /**
     * Get memory usage percentage.
     *
     * @return Memory usage as percentage (0-100)
     */
    public double getMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return (double) used / max * 100;
    }
}
