package tn.esprit.mucroservice.msenrollment.entities;

public enum InstallmentStatus {
    PENDING,    // en attente
    PAID,       // payée
    OVERDUE,    // en retard
    FAILED      // échec de paiement
}