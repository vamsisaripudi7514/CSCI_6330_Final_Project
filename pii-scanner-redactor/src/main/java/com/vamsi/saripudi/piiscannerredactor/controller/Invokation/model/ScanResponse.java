package com.vamsi.saripudi.piiscannerredactor.controller.Invokation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vamsi.saripudi.piiscannerredactor.model.JobStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScanResponse {
    @JsonProperty("job_id")
    public String jobId;
    @JsonProperty("status")
    public JobStatus status;
    @JsonProperty("message")
    public String message;
}
