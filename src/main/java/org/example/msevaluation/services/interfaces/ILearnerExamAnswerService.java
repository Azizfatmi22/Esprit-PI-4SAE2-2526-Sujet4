package org.example.msevaluation.services.interfaces;

import org.example.msevaluation.dto.EvaluationResultDTO;
import org.example.msevaluation.entities.LearnerExamAnswer;

import java.time.LocalDateTime;
import java.util.List;

public interface ILearnerExamAnswerService {
    EvaluationResultDTO saveAllAnswers(List<LearnerExamAnswer> answers);
    List<LearnerExamAnswer> getAnswersByLearnerId(Long learnerId);
    boolean isSubmittingTooFast(LocalDateTime startTime, int durationInMinutes);
}
