package tn.esprit.mucroservice.msenrollment.entities;

public enum InstallmentPlanStatus {
    ACTIVE,      // en cours
    COMPLETED,   // toutes échéances payées
    DEFAULTED    // échéance manquée → accès bloqué
}