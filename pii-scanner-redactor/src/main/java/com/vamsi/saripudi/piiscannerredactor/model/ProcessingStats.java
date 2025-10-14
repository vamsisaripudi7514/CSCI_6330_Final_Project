package com.vamsi.saripudi.piiscannerredactor.model;

import lombok.Builder;
import lombok.Data;

/**
 * Performance statistics for scan job processing
 */
@Data
@Builder
public class ProcessingStats {
    private final int totalFiles;
    private final int processedFiles;
    private final long totalBytes;
    private final long totalProcessingTimeMs;
    private final double totalProcessingTimeSeconds;
    private final double filesPerSecond;
    private final double megabytesPerSecond;
    private final double averageFileProcessingTimeMs;
    private final int threadCount; // Added thread count for parallel processing analysis
    private final long singleThreadTime;
    private double speedUp;
    /**
     * Get completion percentage
     */
    public double getCompletionPercentage() {
        if (totalFiles <= 0) return 0.0;
        return (double) processedFiles / totalFiles * 100.0;
    }

    /**
     * Get total bytes in MB
     */
    public double getTotalMegabytes() {
        return totalBytes / (1024.0 * 1024.0);
    }

    /**
     * Check if processing is complete
     */
    public boolean isComplete() {
        return processedFiles >= totalFiles;
    }

    /**
     * Calculate speedup compared to single-threaded execution
     */
    public double getSpeedup(long singleThreadTime) {
        if (singleThreadTime <= 0) return 0.0;
        this.speedUp = (double)singleThreadTime / totalProcessingTimeMs;
        return speedUp;
    }

    /**
     * Calculate efficiency (speedup per thread)
     */
    public double getEfficiency(int threadCount) {
        if (threadCount <= 0) return 0.0;
        return (this.speedUp / (double)threadCount)*100;
    }

    /**
     * Calculate theoretical maximum speedup based on Amdahl's Law
     */
    public double getTheoreticalMaxSpeedup(double serialFraction) {
        if (serialFraction >= 1.0) return 1.0;
        return 1.0 / (serialFraction + ((1.0 - serialFraction) / threadCount));
    }

    @Override
    public String toString() {
        return String.format(
            "ProcessingStats{files=%d/%d (%.1f%%), bytes=%.2fMB, time=%.2fs, " +
            "throughput=%.2f files/s, %.2f MB/s, avgFileTime=%.2fms, threads=%d}",
            processedFiles, totalFiles, getCompletionPercentage(),
            getTotalMegabytes(), totalProcessingTimeSeconds,
            filesPerSecond, megabytesPerSecond, averageFileProcessingTimeMs, threadCount
        );
    }
}
