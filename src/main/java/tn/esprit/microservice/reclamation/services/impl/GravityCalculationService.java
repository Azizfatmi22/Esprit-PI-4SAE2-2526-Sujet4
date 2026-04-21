package tn.esprit.microservice.reclamation.services.impl;

import org.springframework.stereotype.Service;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationType;
import tn.esprit.microservice.reclamation.services.interfaces.IGravityCalculationService;

@Service
public class GravityCalculationService implements IGravityCalculationService {

    @Override
    public GravityResult calculateGravity(Reclamation reclamation) {

        int score = 0;
        String text = (reclamation.getSubject() + " " + reclamation.getDescription()).toLowerCase();

        if (text.contains("bloque") || text.contains("bloqué")) score += 4;

        if (reclamation.getType() == ReclamationType.PAYMENT &&
                reclamation.getAmount() != null && reclamation.getAmount() > 100) {
            score += 4;
        }

        if (reclamation.getType() == ReclamationType.ACCESS &&
                text.contains("total")) {
            score += 4;
        }

        if (text.contains("urgent") || text.contains("important")) score += 3;

        if (reclamation.getPriority() != null && reclamation.getPriority() == 1) score += 3;

        if (reclamation.getType() == ReclamationType.TECHNICAL) score += 2;

        // ✅ FIX ICI
        IGravityCalculationService.GravityLevel level;

        if (score >= 7) {
            level = IGravityCalculationService.GravityLevel.CRITICAL;
        } else if (score >= 5) {
            level = IGravityCalculationService.GravityLevel.HIGH;
        } else if (score >= 3) {
            level = IGravityCalculationService.GravityLevel.MEDIUM;
        } else {
            level = IGravityCalculationService.GravityLevel.LOW;
        }

        return new GravityResult(score, level);
    }
}