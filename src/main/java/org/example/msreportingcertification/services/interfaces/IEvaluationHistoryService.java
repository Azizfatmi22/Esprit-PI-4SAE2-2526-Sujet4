package org.example.msreportingcertification.services.interfaces;

import org.example.msreportingcertification.dto.EvaluationResultDTO;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.entities.VigilanceStatus;

import java.util.List;

public interface IEvaluationHistoryService {
    List<EvaluationHistory> getResultsByEvaluation(Long evaluationId);
    EvaluationHistory saveResultFromDto(EvaluationResultDTO dto);
    void updateStatus(Long historyId, VigilanceStatus status);
}
