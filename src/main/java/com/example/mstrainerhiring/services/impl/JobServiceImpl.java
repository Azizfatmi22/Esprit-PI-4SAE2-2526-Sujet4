package com.example.mstrainerhiring.services.impl;

import com.example.mstrainerhiring.entities.Job;
import com.example.mstrainerhiring.entities.PartnerHiring;
import com.example.mstrainerhiring.exception.ResourceNotFoundException;
import com.example.mstrainerhiring.repositories.JobRepository;
import com.example.mstrainerhiring.repositories.PartnerHiringRepository;
import com.example.mstrainerhiring.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final PartnerHiringRepository partnerRepository;
    private final com.example.mstrainerhiring.services.TrainerHiringService trainerService;
    private final com.example.mstrainerhiring.repositories.TrainerHiringRepository trainerRepository;

    @Override
    public Job createJob(Job job, UUID partnerId) {
        PartnerHiring partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", "id", partnerId));
        job.setPartner(partner);
        return jobRepository.save(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Job getJobById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getJobsByPartner(UUID partnerId) {
        return jobRepository.findByPartnerId(partnerId);
    }

    @Override
    public void deleteJob(UUID id) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job", "id", id);
        }

        // Deep Delete: Delete all trainer applications associated with this job
        List<com.example.mstrainerhiring.entities.TrainerHiring> applications = trainerRepository.findByJobId(id);
        for (com.example.mstrainerhiring.entities.TrainerHiring application : applications) {
            trainerService.deleteTrainer(application.getId());
        }

        jobRepository.deleteById(id);
    }
}
