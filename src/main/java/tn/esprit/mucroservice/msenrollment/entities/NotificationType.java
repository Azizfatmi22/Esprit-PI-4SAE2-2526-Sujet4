package tn.esprit.mucroservice.msenrollment.entities;

public enum NotificationType {
    PAYMENT_SUCCESS,        // Email après paiement réussi
    CART_ABANDONED,         // Rappel panier abandonné (+24h)
    INSTALLMENT_REMINDER,   // Rappel échéance à venir
    INSTALLMENT_OVERDUE,    // Échéance en retard
    COURSE_ADDED_TO_CART    // Cours ajouté au panier
}