package org.example.msevaluation.services.interfaces;

import org.example.msevaluation.entities.Evaluation;

import java.time.LocalDateTime;
import java.util.List;

public interface IEvaluationService {
    Evaluation saveEvaluation(Evaluation evaluation);
    List<Evaluation> getAllEvaluations();
    Evaluation getEvaluationById(Long id);
    Evaluation updateEvaluation(Long id, Evaluation evaluation);
    void deleteEvaluation(Long id);
    Double getGlobalSuccessRate();
    void updateEvaluationDate(Long id, LocalDateTime newDate);

}
