package com.example.mstrainerhiring.services;

import com.example.mstrainerhiring.entities.Job;
import java.util.List;
import java.util.UUID;

public interface JobService {
    Job createJob(Job job, UUID partnerId);

    List<Job> getAllJobs();

    Job getJobById(UUID id);

    List<Job> getJobsByPartner(UUID partnerId);

    void deleteJob(UUID id);
}
