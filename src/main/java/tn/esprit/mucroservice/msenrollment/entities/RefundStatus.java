package tn.esprit.mucroservice.msenrollment.entities;

public enum RefundStatus {
    PENDING,    // En attente
    PROCESSED,  // Remboursé
    REJECTED    // Refusé (délai 48h dépassé)
}