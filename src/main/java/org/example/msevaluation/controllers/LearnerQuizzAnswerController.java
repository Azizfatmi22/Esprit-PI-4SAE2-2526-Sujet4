package org.example.msevaluation.controllers;

import lombok.RequiredArgsConstructor;
import org.example.msevaluation.dto.EvaluationResultDTO;
import org.example.msevaluation.entities.LearnerQuizzAnswer;
import org.example.msevaluation.services.interfaces.ILearnerQuizzAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/evaluations/quiz/answers")
@RequiredArgsConstructor
public class LearnerQuizzAnswerController {
    private final ILearnerQuizzAnswerService learnerQuizzAnswerService;

    @PostMapping("/submit")
    public ResponseEntity<EvaluationResultDTO> submitQuiz(@RequestBody List<LearnerQuizzAnswer> answers) {
        return ResponseEntity.ok(learnerQuizzAnswerService.saveAllQuizAnswers(answers));
    }
}
