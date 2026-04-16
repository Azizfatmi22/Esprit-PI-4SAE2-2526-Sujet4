package com.example.mstrainerhiring.services;

import com.example.mstrainerhiring.dto.TopCandidateDTO;
import java.util.UUID;

public interface RecommendationService {
    TopCandidateDTO getTopCandidateForJob(UUID jobId);
}
