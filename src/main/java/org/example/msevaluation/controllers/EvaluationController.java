package org.example.msevaluation.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.msevaluation.dto.EvaluationStatsDTO;
import org.example.msevaluation.entities.Evaluation;
import org.example.msevaluation.entities.TypeAssessment;
import org.example.msevaluation.repositories.EvaluationRepository;
import org.example.msevaluation.services.interfaces.IEvaluationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final IEvaluationService evaluationService;
    private final EvaluationRepository evaluationRepository;

    @PostMapping
    public ResponseEntity<Evaluation> createEvaluation(@Valid @RequestBody Evaluation evaluation) {
        Evaluation saved = evaluationService.saveEvaluation(evaluation);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/trainer/{trainerId}")
    public List<Evaluation> getByTrainer(@PathVariable String trainerId) {
        return evaluationRepository.findByTrainerId(trainerId);
    }

    @GetMapping
    public List<Evaluation> getAll() {
        return evaluationService.getAllEvaluations();
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<Evaluation> getDetailsForUpdate(@PathVariable Long id) {
        Evaluation eval = evaluationService.getEvaluationById(id);
        return ResponseEntity.ok(eval);
    }

    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<Evaluation> rescheduleEvaluation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        String dateStr = (String) updates.get("date");
        LocalDateTime newDate = LocalDateTime.parse(dateStr);

        evaluationService.updateEvaluationDate(id, newDate);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Evaluation eval = evaluationService.getEvaluationById(id);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = eval.getDate();
        LocalDateTime endTime = startTime.plusMinutes(eval.getDuration());

        Map<String, Object> errorBody = new HashMap<>();

        if (now.isBefore(startTime)) {
            errorBody.put("message", "L'examen n'a pas encore commencé.");
            errorBody.put("startTime", startTime);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody);
        }

        if (now.isAfter(endTime)) {
            errorBody.put("message", "La session de cet examen est terminée.");
            errorBody.put("endTime", endTime);
            return ResponseEntity.status(HttpStatus.GONE).body(errorBody);
        }

        return ResponseEntity.ok(eval);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        evaluationService.deleteEvaluation(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Evaluation> update(@PathVariable Long id, @RequestBody Evaluation evaluation) {
        try {
            Evaluation updated = evaluationService.updateEvaluation(id, evaluation);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/admin/stats")
    public ResponseEntity<EvaluationStatsDTO> getAdminStats() {
        long totalEvaluations = evaluationRepository.count();
        long totalQuizzes = evaluationRepository.countByTypeAssessment(TypeAssessment.QUIZ);
        long totalExams = evaluationRepository.countByTypeAssessment(TypeAssessment.EXAM);
        double avgSuccessRate = evaluationService.getGlobalSuccessRate();

        return ResponseEntity.ok(new EvaluationStatsDTO(totalEvaluations, totalQuizzes, totalExams, avgSuccessRate));
    }
}
