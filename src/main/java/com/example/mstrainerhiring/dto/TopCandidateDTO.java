package com.example.mstrainerhiring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.mstrainerhiring.entities.TrainerHiring;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCandidateDTO {
    private TrainerHiringDTO candidate;
    private Double globalScore;
    private String recommendationReason;
}
