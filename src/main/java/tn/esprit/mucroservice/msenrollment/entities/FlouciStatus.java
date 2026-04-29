package tn.esprit.mucroservice.msenrollment.entities;

public enum FlouciStatus {
    PENDING_OTP,  // OTP envoyé, en attente de vérification
    VERIFIED,     // OTP confirmé, paiement réussi
    FAILED,       // OTP incorrect
    EXPIRED       // OTP expiré (2 minutes dépassées)
}