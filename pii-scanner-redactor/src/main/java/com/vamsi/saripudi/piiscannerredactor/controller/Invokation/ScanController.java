package com.vamsi.saripudi.piiscannerredactor.controller.Invokation;

import com.vamsi.saripudi.piiscannerredactor.controller.Invokation.model.ScanRequest;
import com.vamsi.saripudi.piiscannerredactor.controller.Invokation.model.ScanResponse;
import com.vamsi.saripudi.piiscannerredactor.model.JobStatus;
import com.vamsi.saripudi.piiscannerredactor.model.ScanJob;
import com.vamsi.saripudi.piiscannerredactor.service.ScanOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/scan")
public class ScanController {

    @Autowired
    private final ScanOrchestrator scanOrchestrator;
    @Autowired
    private ScanJob scanJob;

    public ScanController(ScanOrchestrator scanOrchestrator) {
        this.scanOrchestrator = scanOrchestrator;
    }

    @PostMapping("/submit")
    public ResponseEntity<ScanResponse> submitScan(@RequestBody ScanRequest scanRequest){
        List<Path> paths;
        try{
            Path inputPath = Path.of(scanRequest.getInputPath());
             paths= List.of(inputPath);
        }
        catch(Exception e){
            ScanResponse scanResponse = ScanResponse.builder()
                    .message("Bad Request: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(scanResponse);
        }
        try{
            ScanJob scanJob = scanOrchestrator.submit(paths);
            if(scanJob.getStatus() == JobStatus.COMPLETED || scanJob.getStatus() == JobStatus.RUNNING ||
            scanJob.getStatus() == JobStatus.PENDING){
                ScanResponse scanResponse = ScanResponse.builder()
                        .message("Scan completed is in execution")
                        .jobId(scanJob.getId())
                        .status(scanJob.getStatus())
                        .build();
                return ResponseEntity.ok(scanResponse);
            }
        }
        catch(IOException e){
            ScanResponse scanResponse = ScanResponse.builder()
                    .message("Internal Server Error: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(scanResponse);
        }
        return ResponseEntity.notFound().build();
//        (ScanResponse.builder().message("Not Found").build());
    }
}
