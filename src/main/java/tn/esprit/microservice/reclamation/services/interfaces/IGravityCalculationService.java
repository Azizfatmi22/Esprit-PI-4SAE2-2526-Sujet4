package tn.esprit.microservice.reclamation.services.interfaces;

import tn.esprit.microservice.reclamation.entities.Reclamation;

public interface IGravityCalculationService {

    GravityResult calculateGravity(Reclamation reclamation);

    enum GravityLevel {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    class GravityResult {
        private int score;
        private GravityLevel level;

        public GravityResult(int score, GravityLevel level) {
            this.score = score;
            this.level = level;
        }

        public int getScore() { return score; }
        public GravityLevel getLevel() { return level; }
    }
}