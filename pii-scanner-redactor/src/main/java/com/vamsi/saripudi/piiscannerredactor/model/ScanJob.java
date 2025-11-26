package com.vamsi.saripudi.piiscannerredactor.model;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Builder
@Data
@AllArgsConstructor
@Component
public class ScanJob {

    private final String id;
    private volatile JobStatus status = JobStatus.PENDING;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile String error;

    private Path inputRoot;
    private Path outputRoot;
    private Path redactedRoot;
    private Path findingsCsv;
    private Path summaryPath;

    private volatile int filesTotal;
    private final AtomicInteger filesScanned = new AtomicInteger(0);
    private final AtomicLong bytesScanned = new AtomicLong(0L);

    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0L);
    private final Map<String, Long> fileProcessingTimes = new ConcurrentHashMap<>();
    private volatile long scanStartTimeMs;
    private volatile long scanEndTimeMs;
    @Value("${thread.count-for-speedup}")
    private static int threadCount;
    @Value("${variables.single-thread-time:10}")
    private static long singleThreadTime;

    @Autowired
    public ScanJob(@Value("${thread.count}") int threadCount, @Value("${variables.single-thread-time}") long singleThreadTime) {
        this.id = UUID.randomUUID().toString();
        this.threadCount = threadCount-1;
        this.singleThreadTime = singleThreadTime;
        System.out.println("Thread count: " + threadCount);
        // Other fields are initialized by Lombok or default values
    }

//    private volatile int threadCount; // Track thread count for speedup calculation
    private Path findingsJsonl;

    // Optional per-type summary (safe for concurrent updates)
    private final Map<MatchType, AtomicInteger> summaryByType =
            new ConcurrentHashMap<>();

    // --- Control flags ---
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);


    //New ScanJob creation method
    public static ScanJob create(List<Path> inputs, Path outputRoot) {
        Objects.requireNonNull(outputRoot, "outputRoot");
        String id = UUID.randomUUID().toString();
        ScanJob j = new ScanJob(id,threadCount,singleThreadTime);

        j.outputRoot = outputRoot;
        j.redactedRoot = outputRoot.resolve("redacted");
        j.findingsCsv = outputRoot.resolve("findings.csv");
        j.findingsJsonl = outputRoot.resolve("findings.jsonl");
        j.summaryPath = outputRoot.resolve("summary.json");

        if (inputs != null && !inputs.isEmpty()) {
            Path first = inputs.get(0).toAbsolutePath().normalize();
            j.inputRoot = first.getParent();
            j.filesTotal = inputs.size();
        }
        return j;
    }

    private ScanJob(String id,int threadCount,long singleThreadTime) {
        this.id = id;
        this.threadCount = threadCount;
        this.singleThreadTime = singleThreadTime;
    }


    // --- Lifecycle transitions ---

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
        this.scanStartTimeMs = System.currentTimeMillis();
    }

    /** Mark COMPLETED unless cancel was requested (then CANCELED). */
    public void markCompletedIfNotCanceled() {
        this.status = cancelRequested.get() ? JobStatus.CANCELED : JobStatus.COMPLETED;
        this.finishedAt = Instant.now();
        this.scanEndTimeMs = System.currentTimeMillis();
        totalProcessingTimeMs.set(scanEndTimeMs - scanStartTimeMs);
    }

    public void markFailed(Throwable t) {
        this.status = JobStatus.FAILED;
        this.finishedAt = Instant.now();
        this.scanEndTimeMs = System.currentTimeMillis();
        totalProcessingTimeMs.set(scanEndTimeMs - scanStartTimeMs);
        if (t == null) {
            this.error = "unknown";
        } else {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            t.printStackTrace(pw);
            this.error = sw.toString();
        }
    }



    // --- Progress helpers ---

    public void addBytes(long n) {
        if (n > 0) bytesScanned.addAndGet(n);
    }

    public int incFilesScanned() {
        return filesScanned.incrementAndGet();
    }

    public void setFilesTotal(int total) {
        this.filesTotal = Math.max(0, total);
    }

    public void bumpTypeCount(MatchType type, int delta) {
        if (type == null || delta == 0) return;
        summaryByType.computeIfAbsent(type, k -> new AtomicInteger())
                .addAndGet(delta);
    }

    // --- Cancellation ---

    public void requestCancel() {
        cancelRequested.set(true);
    }

    public boolean isCancelRequested() {
        return cancelRequested.get();
    }

    public Map<MatchType, Integer> getSummaryByTypeSnapshot() {
        Map<MatchType, Integer> snap = new EnumMap<>(MatchType.class);
        for (var e : summaryByType.entrySet()) {
            snap.put(e.getKey(), e.getValue().get());
        }
        return snap;
    }

    // --- Timing and Performance Methods ---

    /**
     * Record the processing time for a specific file
     */
    public void recordFileProcessingTime(String fileName, long processingTimeMs) {
        fileProcessingTimes.put(fileName, processingTimeMs);
    }

    /**
     * Set the number of threads used for this scan job
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = Math.max(1, threadCount);
    }

    /**
     * Get the scan start time in milliseconds (for debugging)
     */
    public long getScanStartTimeMs() {
        return scanStartTimeMs;
    }

    /**
     * Get the scan end time in milliseconds (for debugging)
     */
    public long getScanEndTimeMs() {
        return scanEndTimeMs;
    }

    /**
     * Get the total processing time in milliseconds
     * Calculates dynamically if job is still running
     */
    public long getTotalProcessingTimeMs() {
        // If job is completed, use the stored value
        long storedTime = totalProcessingTimeMs.get();
        if (storedTime > 0) {
            return storedTime;
        }

        // If job is still running or stored time is 0, calculate dynamically
        if (scanStartTimeMs > 0) {
            long endTime = scanEndTimeMs > 0 ? scanEndTimeMs : System.currentTimeMillis();
            return endTime - scanStartTimeMs;
        }

        return 0;
    }

    /**
     * Get the total processing time in seconds
     */
    public double getTotalProcessingTimeSeconds() {
        return getTotalProcessingTimeMs() / 1000.0;
    }

    /**
     * Calculate files processed per second (throughput)
     */
    public double getFilesPerSecond() {
        long totalMs = getTotalProcessingTimeMs();
        if (totalMs <= 0) return 0.0;
        return (double) filesScanned.get() / (totalMs / 1000.0);
    }

    /**
     * Calculate megabytes processed per second (throughput)
     */
    public double getMegabytesPerSecond() {
        long totalMs = getTotalProcessingTimeMs();
        if (totalMs <= 0) return 0.0;
        double totalMB = bytesScanned.get() / (1024.0 * 1024.0);
        return totalMB / (totalMs / 1000.0);
    }

    /**
     * Get average processing time per file in milliseconds
     */
    public double getAverageFileProcessingTimeMs() {
        int scanned = filesScanned.get();
        if (scanned <= 0) return 0.0;
        return (double) getTotalProcessingTimeMs() / scanned;
    }

    /**
     * Calculate speedup compared to single-threaded execution
     * Speedup = T_sequential / T_parallel
     */
    public double getSpeedup(double sequentialTimeMs) {
        long parallelTime = getTotalProcessingTimeMs();
        if (parallelTime <= 0) return 0.0;
        return sequentialTimeMs / parallelTime;
    }

    /**
     * Calculate efficiency (speedup per processor)
     * Efficiency = Speedup / Number_of_Processors
     */
    public double getEfficiency(double speedup) {
        if (threadCount <= 0) return 0.0;
        return speedup / threadCount;
    }

    /**
     * Calculate theoretical maximum speedup (based on Amdahl's Law)
     * assuming some portion of work cannot be parallelized
     */
    public double getTheoreticalMaxSpeedup(double serialFraction) {
        if (serialFraction >= 1.0) return 1.0;
        return 1.0 / (serialFraction + ((1.0 - serialFraction) / threadCount));
    }

    /**
     * Get the processing time for a specific file
     */
    public Long getFileProcessingTime(String fileName) {
        return fileProcessingTimes.get(fileName);
    }

    /**
     * Get all file processing times (copy for thread safety)
     */
    public Map<String, Long> getAllFileProcessingTimes() {
        return new ConcurrentHashMap<>(fileProcessingTimes);
    }


    public static int getThreadCount() {
        return threadCount;
    }

    public static long getSingleThreadTime() {
        return singleThreadTime;
    }

    /**
     * Get processing statistics summary
     */
    public ProcessingStats getProcessingStats() {
        return ProcessingStats.builder()
                .totalFiles(filesTotal)
                .processedFiles(filesScanned.get())
                .totalBytes(bytesScanned.get())
                .totalProcessingTimeMs(getTotalProcessingTimeMs())
                .totalProcessingTimeSeconds(getTotalProcessingTimeSeconds())
                .filesPerSecond(getFilesPerSecond())
                .megabytesPerSecond(getMegabytesPerSecond())
                .averageFileProcessingTimeMs(getAverageFileProcessingTimeMs())
                .threadCount(threadCount)
                .singleThreadTime(singleThreadTime)
                .threadCount(threadCount)
                .build();
    }
}
