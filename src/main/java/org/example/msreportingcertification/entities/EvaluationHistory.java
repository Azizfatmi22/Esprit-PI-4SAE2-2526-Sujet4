package org.example.msreportingcertification.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long evaluationId;
    private String learnerId;
    private String learnerName;
    private int timeSpentSeconds;
    private String evaluationTitle;
    private String type;
    private double scoreObtained;
    private double totalPossiblePoints;
    private int duration;
    private Double percentage;
    private Boolean isPassed;
    private Integer pointsGained;
    @Enumerated(EnumType.STRING)
    private VigilanceStatus vigilanceStatus = VigilanceStatus.CLEAN;

    private LocalDateTime receivedAt = LocalDateTime.now();
    private String certificatePath;


}