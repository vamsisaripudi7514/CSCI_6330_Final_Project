package com.vamsi.saripudi.piiscannerredactor.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ScanJobTest {

    @Test
    public void testScanJobPendingStatus() {
        ScanJob scanJob = ScanJob.create(null, Path.of("/tmp"));
        assertEquals(JobStatus.PENDING, scanJob.getStatus());
    }

    @Test
    public void testScanJobRunningStatus() {
        ScanJob scanJob = ScanJob.create(null, Path.of("/tmp"));
        scanJob.markRunning();
        assertEquals(JobStatus.RUNNING, scanJob.getStatus());
    }

    @Test
    public void testScanJobObject(){
        Path outputRoot = Path.of("/tmp");
        List<Path> inputs = List.of(Path.of("/vamsi/input1.txt"), Path.of("/vamsi/input2.txt"));
        ScanJob scanJob = ScanJob.create(inputs, outputRoot);
        assertEquals(JobStatus.PENDING, scanJob.getStatus());
        assertEquals(Path.of("/vamsi"),scanJob.getInputRoot());
        List<Path> inputs2 = List.of(Path.of("/r/input1.txt"), Path.of("/vamsi/input2.txt"));
        ScanJob scanJob2 = ScanJob.create(inputs2, outputRoot);
        assertEquals(Path.of("/r"),scanJob2.getInputRoot());
        assertEquals(2,scanJob2.getFilesTotal());
    }
}
