package com.example.mstrainerhiring.services.impl;

import com.example.mstrainerhiring.dto.TopCandidateDTO;
import com.example.mstrainerhiring.entities.TrainerHiring;
import com.example.mstrainerhiring.enums.TrainerStatus;
import com.example.mstrainerhiring.repositories.TrainerHiringRepository;
import com.example.mstrainerhiring.services.RecommendationService;
import com.example.mstrainerhiring.mapper.TrainerHiringMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final TrainerHiringRepository trainerHiringRepository;
    private final TrainerHiringMapper trainerHiringMapper;

    @Override
    public TopCandidateDTO getTopCandidateForJob(UUID jobId) {
        log.info("Starting Global Match Algorithm for Job ID: {}", jobId);

        // Fetch all pending candidates for this job
        List<TrainerHiring> pendingCandidates = trainerHiringRepository.findByJobIdAndStatus(jobId,
                TrainerStatus.PENDING);

        if (pendingCandidates == null || pendingCandidates.isEmpty()) {
            return TopCandidateDTO.builder()
                    .globalScore(0.0)
                    .recommendationReason("No pending applications found for this job.")
                    .build();
        }

        TrainerHiring bestCandidate = null;
        double highestScore = -1.0;

        for (TrainerHiring candidate : pendingCandidates) {
            double currentScore = calculateGlobalFitScore(candidate);

            if (currentScore > highestScore) {
                highestScore = currentScore;
                bestCandidate = candidate;
            } else if (currentScore == highestScore && currentScore > 0) {
                // Tie-breaking: Favor better communication if scores are equal
                double currentTone = candidate.getToneClarityScore() != null ? candidate.getToneClarityScore() : 0.0;
                double bestTone = bestCandidate.getToneClarityScore() != null ? bestCandidate.getToneClarityScore() : 0.0;
                if (currentTone > bestTone) {
                    bestCandidate = candidate;
                }
            }
        }

        if (bestCandidate == null || highestScore < 0) {
            return TopCandidateDTO.builder()
                    .globalScore(0.0)
                    .recommendationReason("All candidates were disqualified due to plagiarism or blank CVs.")
                    .build();
        }

        return TopCandidateDTO.builder()
                .candidate(trainerHiringMapper.toDTO(bestCandidate))
                .globalScore(highestScore)
                .recommendationReason(generateReasoning(bestCandidate, highestScore))
                .build();
    }

    private double calculateGlobalFitScore(TrainerHiring candidate) {
        // Disqualify instantly if blank CV
        if (Boolean.TRUE.equals(candidate.getIsBlankCv())) {
            return -1.0;
        }

        double skillScore = candidate.getSkillSyncScore() != null ? candidate.getSkillSyncScore() : 0.0;
        double acceptanceProb = candidate.getAcceptanceProbability() != null ? candidate.getAcceptanceProbability()
                : 0.0;
        double toneScore = candidate.getToneClarityScore() != null ? candidate.getToneClarityScore() : 0.0;

        // Weights: Skill (50%), Trend (30%), Communication Tone (20%)
        double globalScore = (skillScore * 0.50) + (acceptanceProb * 0.30) + (toneScore * 0.20);

        // Heavy penalty for linguistic overlap
        if (Boolean.TRUE.equals(candidate.getPlagiarismFlag())) {
            globalScore -= 50.0;
        }

        return Math.max(0, globalScore); // Don't return negatives unless disqualified
    }

    private String generateReasoning(TrainerHiring candidate, double score) {
        if (score >= 80) {
            return String.format(
                    "%s is an exceptional match (%.1f%% global fit). Their technical skills align strongly with this job's precise requirements.",
                    candidate.getName(), score);
        } else if (score >= 50) {
            return String.format(
                    "%s is a solid candidate (%.1f%% global fit) with adequate experience. Good potential for further interviews.",
                    candidate.getName(), score);
        } else {
            return String.format(
                    "%s scored %.1f%% overall. Consider reviewing their communication style or experience depth before accepting.",
                    candidate.getName(), score);
        }
    }
}
