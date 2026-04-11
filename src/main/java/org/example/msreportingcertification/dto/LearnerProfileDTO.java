package org.example.msreportingcertification.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LearnerProfileDTO {
    private String learnerId;
    private String learnerName;
    private Long totalXp;
    private Integer currentLevel;
    private Integer totalCertificates;
    private double progressToNextLevel;
    private List<String> badgeNames;
}
