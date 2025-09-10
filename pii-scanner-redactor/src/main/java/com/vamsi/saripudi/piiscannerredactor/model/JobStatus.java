package com.vamsi.saripudi.piiscannerredactor.model;

import lombok.ToString;
import org.springframework.stereotype.Component;

@ToString
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED;
}
