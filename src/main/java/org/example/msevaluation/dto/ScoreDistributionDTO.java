package org.example.msevaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ScoreDistributionDTO {
    private String range;      // Ex: "0-20%", "21-40%"
    private long count;        // Nombre d'étudiants dans cette tranche
    private double percentage; // Pourcentage par rapport au total (pour l'UI)
}