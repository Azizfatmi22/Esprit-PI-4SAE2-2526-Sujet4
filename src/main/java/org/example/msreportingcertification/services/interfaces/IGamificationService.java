package org.example.msreportingcertification.services.interfaces;

import org.example.msreportingcertification.entities.EvaluationHistory;
import org.springframework.transaction.annotation.Transactional;

public interface IGamificationService {
    @Transactional
    void processGamification(EvaluationHistory history);

    @Transactional
    void rollbackGamification(EvaluationHistory history);

    Integer getLearnerLevel(String learnerId);
}
