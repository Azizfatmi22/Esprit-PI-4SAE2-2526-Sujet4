package org.example.msevaluation.services.interfaces;

import org.example.msevaluation.dto.EvaluationResultDTO;
import org.example.msevaluation.dto.QuizResultDTO;
import org.example.msevaluation.entities.LearnerQuizzAnswer;

import java.time.LocalDateTime;
import java.util.List;

public interface ILearnerQuizzAnswerService {

    EvaluationResultDTO saveAllQuizAnswers(List<LearnerQuizzAnswer> answers);
    boolean isSubmittingTooFast(LocalDateTime startTime, int durationInMinutes);
}
