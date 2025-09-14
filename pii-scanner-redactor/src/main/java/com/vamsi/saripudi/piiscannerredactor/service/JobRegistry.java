package com.vamsi.saripudi.piiscannerredactor.service;

import com.vamsi.saripudi.piiscannerredactor.model.ScanJob;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobRegistry {

    private final Map<String, ScanJob> jobs = new ConcurrentHashMap<>();

    public void create(ScanJob job) {
        jobs.put(job.getId(), job);
    }

    public Optional<ScanJob> get(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public void update(ScanJob job) {
        jobs.put(job.getId(), job);
    }

    public void cancel(String id) {
        get(id).ifPresent(ScanJob::requestCancel);
    }

    public boolean isRunning(String id) {
        return get(id).map(j -> j.getStatus().name().equals("RUNNING")).orElse(false);
    }

    public int count() {
        return jobs.size();
    }
}
