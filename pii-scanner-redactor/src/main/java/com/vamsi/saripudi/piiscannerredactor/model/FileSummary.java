package com.vamsi.saripudi.piiscannerredactor.model;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public final class FileSummary {
    private final Path file;
    private final long bytes;
    private final boolean binary;
    private final List<DetectionResult> findings;
    private final String redactedContent;

    private FileSummary(Path file, long bytes, boolean binary,
                        List<DetectionResult> findings, String redactedContent) {
        this.file = file;
        this.bytes = Math.max(0L, bytes);
        this.binary = binary;
        this.findings = findings == null ? List.of() : List.copyOf(findings);
        this.redactedContent = redactedContent;
    }

    public static FileSummary text(Path file, long bytes,
                                   List<DetectionResult> findings,
                                   String redactedContent) {
        return new FileSummary(file, bytes, false, findings, redactedContent);
    }

    public static FileSummary text(Path file,
                                   List<DetectionResult> findings,
                                   String redactedContent) {
        return text(file, 0L, findings, redactedContent);
    }

    public static FileSummary binary(Path file, long bytes) {
        return new FileSummary(file, bytes, true, List.of(), null);
    }

    public int getFindingCount() { return findings.size(); }
}
