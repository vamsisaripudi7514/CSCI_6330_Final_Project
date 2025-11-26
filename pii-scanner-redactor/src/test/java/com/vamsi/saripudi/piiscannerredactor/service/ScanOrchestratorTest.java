package com.vamsi.saripudi.piiscannerredactor.service;

import com.vamsi.saripudi.piiscannerredactor.model.JobStatus;
import com.vamsi.saripudi.piiscannerredactor.model.ScanJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ScanOrchestratorTest {

    @Autowired
    ScanOrchestrator scanOrchestrator;

    @Test
    public void TestScan() throws Exception {
        // Use a test corpus directory as input
        Path inputDir = Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/test-corpus");
        List<Path> inputs = List.of(inputDir);

        // Submit the scan job
        ScanJob job = scanOrchestrator.submit(inputs);

        // Wait for job to complete (polling)
        int maxWaitSeconds = 30;
        int waited = 0;
        while (job.getStatus() != JobStatus.COMPLETED && job.getStatus() != JobStatus.FAILED && waited < maxWaitSeconds) {
            Thread.sleep(1000);
            waited++;
        }

        if (job.getStatus() == JobStatus.FAILED) {
            System.err.println("Scan job failed with error: " + job.getError());
        }
        assertEquals(JobStatus.COMPLETED, job.getStatus(), "Job should complete successfully");
        assertEquals(job.getFilesTotal(), job.getFilesScanned().get(), "Files scanned should match total files");
        assertTrue(job.getBytesScanned().get() > 0, "Bytes scanned should be greater than zero");
        assertTrue(Files.exists(job.getFindingsCsv()), "Findings CSV should exist");
        assertTrue(Files.exists(job.getFindingsJsonl()), "Findings JSONL should exist");
        assertTrue(Files.exists(job.getRedactedRoot()), "Redacted output directory should exist");
    }

    @Test
    public void TestScanNew() throws Exception {
        // Use a test corpus directory as input
        Path inputDir = Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/huge_corpus");
        List<Path> inputs = List.of(inputDir);

        // Submit the scan job
        ScanJob job = scanOrchestrator.submit(inputs);

        // Wait for job to complete (polling)
        // Wait until job is completed or failed
        while (job.getStatus() != JobStatus.COMPLETED && job.getStatus() != JobStatus.FAILED) {
            Thread.sleep(1000);
        }

        if (job.getStatus() == JobStatus.FAILED) {
            System.err.println("Scan job failed with error: " + job.getError());
        }
        assertEquals(JobStatus.COMPLETED, job.getStatus(), "Job should complete successfully");
        assertEquals(job.getFilesTotal(), job.getFilesScanned().get(), "Files scanned should match total files");
        assertTrue(job.getBytesScanned().get() > 0, "Bytes scanned should be greater than zero");
        assertTrue(Files.exists(job.getFindingsCsv()), "Findings CSV should exist");
        assertTrue(Files.exists(job.getFindingsJsonl()), "Findings JSONL should exist");
        assertTrue(Files.exists(job.getRedactedRoot()), "Redacted output directory should exist");
    }

    @Test
    public void AnalysisScans() throws Exception {
        // Use a test corpus directory as input
//        Path inputDir = Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/small_corpus");
//        Path inputDir = Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/big_corpus");
        Path inputDir = Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/huge_corpus");

        List<Path> inputs = List.of(inputDir);

        // Submit the scan job
        ScanJob job = scanOrchestrator.submit(inputs);

        // Wait for job to complete (polling)
        // Wait until job is completed or failed
        while (job.getStatus() != JobStatus.COMPLETED && job.getStatus() != JobStatus.FAILED) {
            Thread.sleep(1000);
        }

        if (job.getStatus() == JobStatus.FAILED) {
            System.err.println("Scan job failed with error: " + job.getError());
        }
        assertEquals(JobStatus.COMPLETED, job.getStatus(), "Job should complete successfully");
        assertEquals(job.getFilesTotal(), job.getFilesScanned().get(), "Files scanned should match total files");
        assertTrue(job.getBytesScanned().get() > 0, "Bytes scanned should be greater than zero");
        assertTrue(Files.exists(job.getFindingsCsv()), "Findings CSV should exist");
        assertTrue(Files.exists(job.getFindingsJsonl()), "Findings JSONL should exist");
        assertTrue(Files.exists(job.getRedactedRoot()), "Redacted output directory should exist");
    }
}
