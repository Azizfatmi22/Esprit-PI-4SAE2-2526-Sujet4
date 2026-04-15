package org.example.msevaluation.controllers;

import lombok.RequiredArgsConstructor;
import org.example.msevaluation.dto.EvaluationResultDTO;
import org.example.msevaluation.entities.LearnerExamAnswer;
import org.example.msevaluation.services.interfaces.ILearnerExamAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/evaluations/exam/answers")
@RequiredArgsConstructor
public class LearnerExamAnswerController {
    private final ILearnerExamAnswerService learnerExamAnswerService;

    @PostMapping("/submit")
    public ResponseEntity<EvaluationResultDTO> submit(@RequestBody List<LearnerExamAnswer> answers) {
        return ResponseEntity.ok(learnerExamAnswerService.saveAllAnswers(answers));
    }
}
