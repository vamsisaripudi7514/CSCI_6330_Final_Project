package com.vamsi.saripudi.piiscannerredactor.service;

import com.vamsi.saripudi.piiscannerredactor.model.ScanJob;
import com.vamsi.saripudi.piiscannerredactor.model.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;


@Service
@RequiredArgsConstructor
public class ScanOrchestrator {

    private final ExecutorService fileExecutor; // from ThreadConfig (virtual threads)
    private final JobRegistry registry;
    private final ScannerService scanner;


    public ScanJob submit(List<Path> inputs) throws IOException {
        Path jobsRoot = Path.of("jobs");
        Files.createDirectories(jobsRoot);

        Path outputRoot = Files.createTempDirectory(jobsRoot, "job_");

        ScanJob job = ScanJob.create(inputs, outputRoot);
        Files.createDirectories(job.getOutputRoot());
        Files.createDirectories(job.getRedactedRoot());
        registry.create(job);

        fileExecutor.submit(() -> {
            try {
                job.markRunning();
                scanner.scan(job, inputs);
                job.markCompletedIfNotCanceled();
            } catch (Throwable t) {
                job.markFailed(t);
            } finally {
                registry.update(job);
            }
        });

        return job;
    }

    public void cancel(String jobId) {
        registry.get(jobId).ifPresent(ScanJob::requestCancel);
    }

    public JobStatus statusOf(String jobId) {
        return registry.get(jobId).map(ScanJob::getStatus).orElse(null);
    }
}
