package com.vamsi.saripudi.piiscannerredactor.controller.Job;

import com.vamsi.saripudi.piiscannerredactor.controller.Job.model.GetJobResponse;
import com.vamsi.saripudi.piiscannerredactor.controller.Job.model.GetPathResponse;
import com.vamsi.saripudi.piiscannerredactor.model.ScanJob;
import com.vamsi.saripudi.piiscannerredactor.service.JobRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/job")
public class JobController {
    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobService jobService;

    @GetMapping("/{id}")
    public ResponseEntity<GetJobResponse> getJob(@PathVariable String id){
        try{
            Optional<ScanJob> job = jobRegistry.get(id);
            if(job.isPresent()){
                return ResponseEntity.ok(GetJobResponse.builder()
                        .jobId(job.get().getId())
                        .status(job.get().getStatus())
                        .message("Job Completed")
                        .build());
            }
            else{
                return ResponseEntity.status(404).body(GetJobResponse.builder()
                        .message("Job Not Found")
                        .build());
            }
        }
        catch(Exception e){
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/get-path/{id}")
    public ResponseEntity<GetPathResponse> getPath(@PathVariable String id){
        try{
            Optional<ScanJob> job = jobRegistry.get(id);
            if(job.isPresent()){
                return ResponseEntity.ok(GetPathResponse.builder()
                        .pathForRedactedFiles(job.get().getRedactedRoot().toString())
                        .pathForSummaryFile(job.get().getFindingsCsv().toString())
                        .message("Paths Found")
                        .build());
            }
            else{
                return ResponseEntity.status(404).body(GetPathResponse.builder()
                        .message("Job Not Found")
                        .build());
            }
        }
        catch(Exception e){
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/get-path-file/{id}")
    public ResponseEntity<Resource> getFindingsFile(@PathVariable String id){
       if (id==null || id.isBlank()){
           return ResponseEntity.badRequest().build();
       }
       try {
           Resource resource = jobService.getFindingsFile(id);
           if (resource == null) {
               return ResponseEntity.notFound().build();
           }

           return ResponseEntity.ok()
                   .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                   .contentLength(resource.contentLength())
                   .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"findings.csv\"")
                   .body(resource);
       } catch (Exception e) {
           return ResponseEntity.status(500).build();
       }
    }
}
