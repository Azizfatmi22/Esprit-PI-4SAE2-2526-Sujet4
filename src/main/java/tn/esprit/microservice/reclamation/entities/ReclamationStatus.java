package tn.esprit.microservice.reclamation.entities;

public enum ReclamationStatus {
    PENDING,      // En attente
    IN_PROGRESS,  // En cours de traitement
    RESOLVED,     // Résolue
    CLOSED,       // Fermée
    REJECTED      // Rejetée
}
