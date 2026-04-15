package org.example.msevaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuizResultDTO {
    private double score;
    private double percentage;
    private boolean isSuccess;

}