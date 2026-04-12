package org.example.msreportingcertification.controllers;

import lombok.RequiredArgsConstructor;
import org.example.msreportingcertification.entities.CertificateTemplate;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.VigilanceStatus;
import org.example.msreportingcertification.repositories.CertificateTemplateRepository;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.example.msreportingcertification.services.interfaces.IEvaluationHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
public class ReportingController {
    private final IEvaluationHistoryService evaluationHistoryService;
    private final EvaluationHistoryRepository evaluationHistoryRepository;
    private final CertificateTemplateRepository certificateTemplateRepository;

    @GetMapping("/evaluation/{idEvaluation}")
    public ResponseEntity<List<EvaluationHistory>> getResults(@PathVariable Long idEvaluation) {
        List<EvaluationHistory> results = evaluationHistoryService.getResultsByEvaluation(idEvaluation);
        return ResponseEntity.ok(results);
    }


    @PatchMapping("/history/{id}/status")
    public ResponseEntity<Void> updateVigilanceStatus(
            @PathVariable Long id,
            @RequestParam VigilanceStatus status) {
        evaluationHistoryService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/find")
    public ResponseEntity<EvaluationHistory> findHistory(@RequestParam String learnerId, @RequestParam Long evaluationId) {
        return evaluationHistoryRepository.findFirstByLearnerIdAndEvaluationIdOrderByReceivedAtDesc(learnerId, evaluationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/history/all")
    public ResponseEntity<List<Map<String, Object>>> getLearnerHistory(@RequestParam String learnerId) {
        List<EvaluationHistory> historyList = evaluationHistoryRepository.findByLearnerId(learnerId);

        List<Map<String, Object>> enrichedData = historyList.stream().map(history -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", history.getId());
            map.put("evaluationTitle", history.getEvaluationTitle());
            map.put("percentage", history.getPercentage());
            map.put("receivedAt", history.getReceivedAt());
            map.put("vigilanceStatus", history.getVigilanceStatus());
            map.put("learnerName", history.getLearnerName());
            map.put("isPassed", history.getIsPassed());

            // Récupération du template associé
            String html = certificateTemplateRepository.findByEvaluationId(history.getEvaluationId())
                    .map(CertificateTemplate::getHtmlContent)
                    .orElse("");

            map.put("templateHtml", html);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(enrichedData);
    }
}
