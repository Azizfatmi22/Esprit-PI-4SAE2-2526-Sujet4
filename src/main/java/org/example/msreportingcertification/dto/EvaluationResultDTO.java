package org.example.msreportingcertification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EvaluationResultDTO {
    private String learnerId;
    private String learnerName; // À récupérer via un appel inter-service ou ID
    private String evaluationTitle;
    private String type;
    private double scoreObtained;
    private double totalPossiblePoints;
    private int duration;
    private int correctAnswersCount;
    private int wrongAnswersCount;
    private int totalQuestions;
    private double percentage;
    private int minSuccessScore;
    private Boolean isPassed;
    private Long evaluationId;
    private Boolean isSuspicious;
    private int timeSpentSeconds;
}
