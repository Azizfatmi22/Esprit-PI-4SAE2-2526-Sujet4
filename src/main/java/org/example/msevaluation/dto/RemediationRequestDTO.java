package org.example.msevaluation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RemediationRequestDTO {
    private Long evaluationId;
    private String evaluationTitle;
    private List<String> criticalTopics;
    private int studentCount;
}
