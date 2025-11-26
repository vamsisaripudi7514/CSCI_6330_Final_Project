package com.vamsi.saripudi.piiscannerredactor.controller.Job.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetPathResponse {
    @JsonProperty("redacted_files_path")
    public String pathForRedactedFiles;

    @JsonProperty("findings_file_path")
    public String pathForSummaryFile;

    @JsonProperty("message")
    public String message;

}
