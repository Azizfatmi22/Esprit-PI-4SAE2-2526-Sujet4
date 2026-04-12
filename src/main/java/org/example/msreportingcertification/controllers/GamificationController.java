package org.example.msreportingcertification.controllers;

import lombok.RequiredArgsConstructor;
import org.example.msreportingcertification.dto.LearnerProfileDTO;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.LearnerProgression;
import org.example.msreportingcertification.entities.UserAchievement;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.example.msreportingcertification.repositories.LearnerProgressionRepository;
import org.example.msreportingcertification.services.interfaces.IBadgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reporting/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final LearnerProgressionRepository progressionRepo;
    private final EvaluationHistoryRepository historyRepo;


    @GetMapping("/profile/{learnerId}")
    public ResponseEntity<LearnerProfileDTO> getProfile(@PathVariable String learnerId) {

        LearnerProgression progression = progressionRepo.findById(learnerId)
                .orElseGet(() -> {
                    String realName = historyRepo.findFirstByLearnerIdOrderByReceivedAtDesc(learnerId)
                            .map(EvaluationHistory::getLearnerName)
                            .orElse("New learner");

                    return LearnerProgression.builder()
                            .learnerId(learnerId)
                            .learnerName(realName)
                            .totalXp(0L)
                            .currentLevel(1)
                            .totalCertificates(0)
                            .build();
                });

        return ResponseEntity.ok(mapToDto(progression));
    }

    private LearnerProfileDTO mapToDto(LearnerProgression p) {
        double progress = ((double) (p.getTotalXp() % 500) / 500) * 100;

        return LearnerProfileDTO.builder()
                .learnerId(p.getLearnerId())
                .learnerName(p.getLearnerName())
                .totalXp(p.getTotalXp())
                .currentLevel(p.getCurrentLevel())
                .totalCertificates(p.getTotalCertificates())
                .progressToNextLevel(progress)
                .badgeNames(new java.util.ArrayList<>())
                .build();
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LearnerProfileDTO>> getLeaderboard() {
        return ResponseEntity.ok(
                progressionRepo.findTop10ByOrderByTotalXpDesc()
                        .stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList())
        );
    }


}