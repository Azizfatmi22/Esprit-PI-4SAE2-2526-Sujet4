package com.example.mstrainerhiring.controllers;

import com.example.mstrainerhiring.dto.JobDTO;
import com.example.mstrainerhiring.entities.Job;
import com.example.mstrainerhiring.mapper.JobMapper;
import com.example.mstrainerhiring.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobMapper jobMapper;

    @PostMapping("/{partnerId}")
    public ResponseEntity<JobDTO> createJob(@PathVariable UUID partnerId, @RequestBody JobDTO jobDTO) {
        Job job = jobMapper.toEntity(jobDTO);
        Job saved = jobService.createJob(job, partnerId);
        return new ResponseEntity<>(jobMapper.toDTO(saved), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<JobDTO>> getAllJobs() {
        List<JobDTO> jobs = jobService.getAllJobs().stream()
                .map(jobMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDTO> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobMapper.toDTO(jobService.getJobById(id)));
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<List<JobDTO>> getJobsByPartner(@PathVariable UUID partnerId) {
        List<JobDTO> jobs = jobService.getJobsByPartner(partnerId).stream()
                .map(jobMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
