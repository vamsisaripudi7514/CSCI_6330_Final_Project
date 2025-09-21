package com.vamsi.saripudi.piiscannerredactor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vamsi.saripudi.piiscannerredactor.model.DetectionResult;
import com.vamsi.saripudi.piiscannerredactor.model.FileSummary;
import com.vamsi.saripudi.piiscannerredactor.model.ScanJob;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ReportingService {

    private final ReentrantLock csvLock = new ReentrantLock();
    private final ReentrantLock jsonlLock = new ReentrantLock();
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Merge a single file's results into the job outputs.
     * - Appends findings to CSV and JSONL
     * - Writes the redacted file to the mirrored path (if present)
     */
    public void merge(ScanJob job, FileSummary summary) throws IOException {
        // 1) Append findings
        appendFindings(job.getFindingsCsv(), job.getFindingsJsonl(), summary.getFindings());

        // 2) Write redacted file (if this was a text file)
        if (summary.getRedactedContent() != null) {
            Path target = job.getRedactedRoot().resolve(relativizeSafe(summary.getFile()));
            Files.createDirectories(target.getParent());
            try (BufferedWriter bw = Files.newBufferedWriter(
                    target, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                bw.write(summary.getRedactedContent());
            }
        }
    }

    /**
     * Called once after all file futures have been collected.
     * Writes a summary.json file with job stats and findings summary.
     */
    public void finalizeOutputs(ScanJob job) {
        try {
            Path summaryPath = job.getOutputRoot().resolve("summary.json");
            var summary = new java.util.LinkedHashMap<String, Object>();
            summary.put("jobId", job.getId());
            summary.put("status", job.getStatus().name());
            summary.put("filesTotal", job.getFilesTotal());
            summary.put("filesScanned", job.getFilesScanned().get());
            summary.put("bytesScanned", job.getBytesScanned().get());
            summary.put("findingsCsv", job.getFindingsCsv().toString());
            summary.put("findingsJsonl", job.getFindingsJsonl().toString());
            summary.put("redactedRoot", job.getRedactedRoot().toString());
            summary.put("summaryByType", job.getSummaryByTypeSnapshot());
            if (job.getError() != null) summary.put("error", job.getError());
            mapper.writerWithDefaultPrettyPrinter().writeValue(summaryPath.toFile(), summary);
        } catch (Exception e) {
            // Log or print error, but don't throw
            System.err.println("Failed to write summary.json: " + e.getMessage());
        }
    }


    private void appendFindings(Path csv, Path jsonl, List<DetectionResult> findings) throws IOException {
        if (findings == null || findings.isEmpty()) return;

        // CSV lock
        csvLock.lock();
        try {
            boolean writeHeader = !Files.exists(csv);
            try (BufferedWriter w = Files.newBufferedWriter(
                    csv, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

                if (writeHeader) {
                    w.write("file,line,startCol,endCol,type,value,score");
                    w.newLine();
                }
                for (DetectionResult r : findings) {
                    w.write(escapeCsv(r.getFilePath() == null ? "" : r.getFilePath().toString()));
                    w.write(',');
                    w.write(Integer.toString(r.getLine()));
                    w.write(',');
                    w.write(Integer.toString(r.getStartCol()));
                    w.write(',');
                    w.write(Integer.toString(r.getEndCol()));
                    w.write(',');
                    w.write(escapeCsv(r.getType() == null ? "" : r.getType().name()));
                    w.write(',');
                    w.write(escapeCsv(r.getValue()));
                    w.write(',');
                    w.write(Double.toString(r.getScore()));
                    w.newLine();
                }
            }
        } finally {
            csvLock.unlock();
        }

        // JSONL (guarded)
        jsonlLock.lock();
        try (BufferedWriter w = Files.newBufferedWriter(
                jsonl, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            for (DetectionResult r : findings) {
                w.write(mapper.writeValueAsString(r));
                w.newLine();
            }
        } finally {
            jsonlLock.unlock();
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        boolean needsQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String out = s.replace("\"", "\"\"");
        return needsQuote ? ("\"" + out + "\"") : out;
    }


    private static Path relativizeSafe(Path file) {
        Path p = file.toAbsolutePath().normalize();
        if (p.getParent() == null) return Path.of(p.getFileName().toString());
        return Path.of(p.getParent().getFileName().toString(), p.getFileName().toString());
    }
}
