package org.example.msevaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EvaluationStatsDTO {
    private long totalEvaluations;
    private long totalQuizzes;
    private long totalExams;
    private double avgSuccessRate;
}
