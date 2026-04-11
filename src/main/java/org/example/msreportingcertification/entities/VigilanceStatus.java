package org.example.msreportingcertification.entities;

public enum VigilanceStatus {
    CLEAN,      // Tout est correct
    SUSPICIOUS, // Détecté automatiquement (ex: trop rapide)
    BANNED      // Marqué manuellement par le formateur (Pas de certificat possible)
}
