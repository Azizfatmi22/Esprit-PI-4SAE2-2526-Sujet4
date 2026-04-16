package com.example.mstrainerhiring.controllers;

import com.example.mstrainerhiring.dto.JobDTO;
import com.example.mstrainerhiring.entities.Job;
import com.example.mstrainerhiring.mapper.JobMapper;
import com.example.mstrainerhiring.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.example.mstrainerhiring.enums.Technology;
import com.example.mstrainerhiring.utils.TemplateGenerator;

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

    @GetMapping("/template")
    public ResponseEntity<JobDTO> getJobTemplate(@RequestParam Technology technology) {
        return ResponseEntity.ok(TemplateGenerator.generateTemplate(technology));
    }

    @GetMapping("/market-sync")
    public ResponseEntity<Map<String, Object>> getMarketSync(@RequestParam Technology technology) {
        List<Job> allJobs = jobService.getAllJobs();
        List<Job> techJobs = allJobs.stream()
                .filter(j -> j.getTechnology() == technology)
                .collect(Collectors.toList());

        Map<String, Object> sync = new HashMap<>();
        
        // 1. Average Experience logic
        if (techJobs.isEmpty()) {
            sync.put("averageExperience", 3.0); 
        } else {
            double avgExp = techJobs.stream()
                    .filter(j -> j.getMinExperience() != null)
                    .mapToInt(Job::getMinExperience)
                    .average()
                    .orElse(3.0);
            sync.put("averageExperience", Math.round(avgExp * 10.0) / 10.0);
        }

        // 2. Predictive Scarcity Index (0-10)
        // Internal logic: If a technology represents more than 40% of all jobs, it's "High Demand" 
        // but if it's less than 10%, it's "Niche/Scarcity".
        double totalJobs = allJobs.size();
        double scarcityIndex;
        if (totalJobs == 0) {
            scarcityIndex = 5.0; // Moderate
        } else {
            double techPercentage = (techJobs.size() / totalJobs) * 100.0;
            if (techPercentage < 10) scarcityIndex = 8.5; // Rare
            else if (techPercentage < 25) scarcityIndex = 6.0; // Moderate
            else scarcityIndex = 3.0; // Abundant
        }
        sync.put("scarcityIndex", scarcityIndex);
        sync.put("demandLevel", scarcityIndex > 7 ? "CRITICAL" : (scarcityIndex > 4 ? "STEADY" : "SATURATED"));
        
        return ResponseEntity.ok(sync);
    }
}
