package com.vamsi.saripudi.piiscannerredactor.controller.Job;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
@Service
public class JobService {

    public Resource getSummaryFile(String id){
        if (id == null || id.isBlank()) return null;

        // normalize: accept either "job_<uuid>" or raw uuid
        String lookupId = id.startsWith("job_") ? id.substring("job_".length()) : id;
        String folderName = "job_" + lookupId;

        // Look for ./jobs/job_<id>/summary.json
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path summaryFile = cwd.resolve("jobs").resolve(folderName).resolve("summary.json");

        if (Files.exists(summaryFile)) {
            return new FileSystemResource(summaryFile);
        }

        return null;
    }

    public Resource getFindingsFile(String id){
        if (id == null || id.isBlank()) return null;

        // normalize: accept either "job_<uuid>" or raw uuid
        String lookupId = id.startsWith("job_") ? id.substring("job_".length()) : id;
        String folderName = "job_" + lookupId;

        // Look for ./jobs/job_<id>/findings.csv
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path findingsFile = cwd.resolve("jobs").resolve(folderName).resolve("findings.csv");

        if (Files.exists(findingsFile)) {
            return new FileSystemResource(findingsFile);
        }

        return null;
    }
}