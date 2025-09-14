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

    private Path filePath;
    private int line;
    private int startCol;
    private int endCol;
    private MatchType type;
    private String value;
    private double score;

}