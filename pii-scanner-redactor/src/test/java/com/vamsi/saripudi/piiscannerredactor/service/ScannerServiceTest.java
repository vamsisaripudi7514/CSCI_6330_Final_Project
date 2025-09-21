package com.vamsi.saripudi.piiscannerredactor.service;

import com.vamsi.saripudi.piiscannerredactor.model.JobStatus;
import com.vamsi.saripudi.piiscannerredactor.model.MatchType;
import com.vamsi.saripudi.piiscannerredactor.model.ScanJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ScannerServiceTest {

    @Autowired
    ScannerService scannerService;



    @Test
    public void TestScan(){
        ScanJob scanjob = ScanJob.builder().
                id("1").
                status(JobStatus.PENDING).
                inputRoot(Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/test-corpus")).
                outputRoot(Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/test-corpus/output")).
                redactedRoot(Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/test-corpus/output/redacted")).
                findingsCsv(Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/test-corpus/output/findings.csv")).
                findingsJsonl(Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/test-corpus/output/findings.jsonl")).
                build();
        scanjob.setStatus(JobStatus.RUNNING);
        try {
            scannerService.scan(scanjob, List.of(Path.of("/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/data-corpus/test-corpus")));

            // Assert that the scan completed as expected
            assertEquals(scanjob.getFilesTotal(), scanjob.getFilesScanned().get(), "Files scanned should match total files");
            assertTrue(scanjob.getBytesScanned().get() > 0, "Bytes scanned should be greater than zero");

            // Assert that output files exist
            assertTrue(java.nio.file.Files.exists(scanjob.getFindingsCsv()), "Findings CSV should exist");
            assertTrue(java.nio.file.Files.exists(scanjob.getFindingsJsonl()), "Findings JSONL should exist");
            assertTrue(java.nio.file.Files.exists(scanjob.getRedactedRoot()), "Redacted output directory should exist");
        }
        catch(Exception c){
            fail(c.getMessage());
        }
    }

}
