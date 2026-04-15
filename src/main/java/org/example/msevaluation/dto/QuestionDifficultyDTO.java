package org.example.msevaluation.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class QuestionDifficultyDTO {
    private Long questionId;
    private String questionText;
    private double successRate;
    private long totalAttempts;
    private String difficultyLevel;
}
