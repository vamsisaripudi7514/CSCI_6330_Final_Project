package com.vamsi.saripudi.piiscannerredactor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetectionResult {

    private String filePath;
    private int line;
    private MatchType type;
    private String value;
    private double score;

    public static DetectionResult of(Path file, int line, MatchType type, String value, double score) {
        return new DetectionResult(file.toString(), line, type, value, score);
    }
}