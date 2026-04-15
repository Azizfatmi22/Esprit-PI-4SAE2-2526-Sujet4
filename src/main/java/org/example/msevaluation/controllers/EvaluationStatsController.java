package org.example.msevaluation.controllers;

import lombok.RequiredArgsConstructor;
import org.example.msevaluation.dto.QuestionDifficultyDTO;
import org.example.msevaluation.dto.ScoreDistributionDTO;
import org.example.msevaluation.entities.Evaluation;
import org.example.msevaluation.repositories.EvaluationRepository;
import org.example.msevaluation.services.impl.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/evaluations/stats")
@RequiredArgsConstructor
public class EvaluationStatsController {
    private final StatisticsService statisticsService;
    private final EvaluationRepository evaluationRepository;


    @PostMapping("/{id}/remediate")
    public ResponseEntity<Void> triggerRemediation(@PathVariable Long id) {
        List<QuestionDifficultyDTO> analysis = statisticsService.getQuestionsAnalysis(id);
        List<String> hardTopics = analysis.stream()
                .filter(q -> "HARD".equals(q.getDifficultyLevel()))
                .map(QuestionDifficultyDTO::getQuestionText)
                .toList();
        System.out.println("Envoi de ressources de révision pour : " + hardTopics);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/questions-analysis")
    public ResponseEntity<List<QuestionDifficultyDTO>> getAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(statisticsService.getQuestionsAnalysis(id));
    }

    @GetMapping("/{id}/distribution")
    public ResponseEntity<List<ScoreDistributionDTO>> getScoreDistribution(@PathVariable Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluation non trouvée"));
        List<ScoreDistributionDTO> distribution = statisticsService.getScoreDistribution(id, eval.getTypeAssessment());

        return ResponseEntity.ok(distribution);
    }
}
