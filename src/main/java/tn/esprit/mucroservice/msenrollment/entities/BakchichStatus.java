package tn.esprit.mucroservice.msenrollment.entities;

public enum BakchichStatus {
    PENDING_CASH,  // En attente de paiement en agence
    CONFIRMED,     // Confirmé par l'agent
    EXPIRED,       // Expiré après 24h
    CANCELLED      // Annulé
}