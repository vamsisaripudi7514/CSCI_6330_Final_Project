package com.vamsi.saripudi.piiscannerredactor.controller.Invokation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScanRequest {
    @JsonProperty("input_path")
    private String inputPath;
}
