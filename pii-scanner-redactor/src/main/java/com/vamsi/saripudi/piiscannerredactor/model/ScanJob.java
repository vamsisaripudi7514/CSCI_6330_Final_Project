package com.vamsi.saripudi.piiscannerredactor.model;

import lombok.*;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Builder
@Data
@AllArgsConstructor
public class ScanJob {

    private final String id;
    private volatile JobStatus status = JobStatus.PENDING;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile String error; // non-null if FAILED

    // --- IO roots & outputs (resolved/created by orchestrator) ---
    private Path inputRoot;        // optional: common parent of first input, if desired
    private Path outputRoot;       // jobs/{id}/output
    private Path redactedRoot;     // jobs/{id}/output/redacted
    private Path findingsCsv;      // jobs/{id}/output/findings.csv
    private Path findingsJsonl;    // jobs/{id}/output/findings.jsonl

    // --- Progress ---
    private volatile int filesTotal;
    private final AtomicInteger filesScanned = new AtomicInteger(0);
    private final AtomicLong bytesScanned = new AtomicLong(0L);

    // Optional per-type summary (safe for concurrent updates)
    private final Map<MatchType, AtomicInteger> summaryByType =
            new ConcurrentHashMap<>();

    // --- Control flags ---
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);


    //New ScanJob creation method
    public static ScanJob create(List<Path> inputs, Path outputRoot) {
        Objects.requireNonNull(outputRoot, "outputRoot");
        String id = UUID.randomUUID().toString();
        ScanJob j = new ScanJob(id);

        j.outputRoot = outputRoot;
        j.redactedRoot = outputRoot.resolve("redacted");
        j.findingsCsv = outputRoot.resolve("findings.csv");
        j.findingsJsonl = outputRoot.resolve("findings.jsonl");

        if (inputs != null && !inputs.isEmpty()) {
            Path first = inputs.get(0).toAbsolutePath().normalize();
            j.inputRoot = first.getParent();
            j.filesTotal = inputs.size();
        }
        return j;
    }

    private ScanJob(String id) { this.id = id; }

    // --- Lifecycle transitions ---

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    /** Mark COMPLETED unless cancel was requested (then CANCELED). */
    public void markCompletedIfNotCanceled() {
        this.status = cancelRequested.get() ? JobStatus.CANCELED : JobStatus.COMPLETED;
        this.finishedAt = Instant.now();
    }

    public void markFailed(Throwable t) {
        this.status = JobStatus.FAILED;
        this.finishedAt = Instant.now();
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
}
