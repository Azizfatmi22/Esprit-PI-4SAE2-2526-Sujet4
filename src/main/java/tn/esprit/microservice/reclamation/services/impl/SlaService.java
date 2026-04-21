package tn.esprit.microservice.reclamation.services.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.reclamation.config.SlaConfig;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.entities.SlaAlert;
import tn.esprit.microservice.reclamation.repositories.SlaAlertRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlaService {

    private final SlaAlertRepository slaAlertRepository;
    private final JavaMailSender mailSender;
    private final SlaConfig slaConfig;

    @Value("${app.admin.email}")
    private String adminEmail;

    // ── Vérifier si une réclamation dépasse son SLA ──────────────────────────
    public boolean isSlaBreached(Reclamation r) {
        if (r.getStatus() == ReclamationStatus.RESOLVED ||
                r.getStatus() == ReclamationStatus.CLOSED   ||
                r.getStatus() == ReclamationStatus.REJECTED) {
            return false;
        }

        int priority  = r.getPriority() != null ? r.getPriority() : 3;
        int slaMin    = slaConfig.getSlaMinutes(priority);
        LocalDateTime deadline = r.getCreatedDate().plusMinutes(slaMin);

        return LocalDateTime.now().isAfter(deadline);
    }

    // ── Calculer les minutes écoulées depuis la création ──────────────────────
    public long getElapsedMinutes(Reclamation r) {
        return ChronoUnit.MINUTES.between(r.getCreatedDate(), LocalDateTime.now());
    }

    // ── Créer une alerte SLA et envoyer l'email ───────────────────────────────
    public void createAlertIfNotExists(Reclamation r) {
        // Eviter les doublons : une seule alerte active par réclamation
        boolean alreadyAlerted = slaAlertRepository
                .findByReclamationIdAndResolvedFalse(r.getId())
                .isPresent();

        if (alreadyAlerted) return;

        int priority     = r.getPriority() != null ? r.getPriority() : 3;
        int slaMin       = slaConfig.getSlaMinutes(priority);
        long elapsedMin  = getElapsedMinutes(r);

        // Sauvegarder l'alerte en base
        SlaAlert alert = SlaAlert.builder()
                .reclamationId(r.getId())
                .priority(priority)
                .slaMinutes(slaMin)
                .elapsedMinutes(elapsedMin)
                .alertDate(LocalDateTime.now())
                .emailSent(false)
                .resolved(false)
                .build();

        SlaAlert saved = slaAlertRepository.save(alert);

        // Envoyer l'email
        try {
            sendSlaEmail(r, slaMin, elapsedMin);
            saved.setEmailSent(true);
            slaAlertRepository.save(saved);
            System.out.println("📧 Email SLA envoyé pour réclamation #" + r.getId());
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email SLA: " + e.getMessage());
        }
    }

    // ── Marquer l'alerte comme résolue quand la réclamation est traitée ───────
    public void resolveAlert(Long reclamationId) {
        slaAlertRepository.findByReclamationIdAndResolvedFalse(reclamationId)
                .ifPresent(alert -> {
                    alert.setResolved(true);
                    slaAlertRepository.save(alert);
                });
    }

    // ── Récupérer toutes les alertes actives ──────────────────────────────────
    public List<SlaAlert> getActiveAlerts() {
        return slaAlertRepository.findByResolvedFalseOrderByAlertDateDesc();
    }

    // ── Envoyer l'email d'alerte ──────────────────────────────────────────────
    private void sendSlaEmail(Reclamation r, int slaMin, long elapsedMin) {
        String priorityLabel = slaConfig.getPriorityLabel(
                r.getPriority() != null ? r.getPriority() : 3
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("🚨 ALERTE SLA — Réclamation #" + r.getId() + " dépasse le délai");
        message.setText(
                "⚠️ ALERTE SLA DÉPASSÉE\n\n" +
                        "Réclamation  : #" + r.getId() + "\n" +
                        "Sujet        : " + r.getSubject() + "\n" +
                        "Type         : " + r.getType() + "\n" +
                        "Priorité     : " + priorityLabel + "\n" +
                        "Statut actuel: " + r.getStatus() + "\n" +
                        "Apprenant    : #" + r.getLearnerId() + "\n\n" +
                        "Délai SLA    : " + slaMin + " minutes\n" +
                        "Temps écoulé : " + elapsedMin + " minutes\n" +
                        "Dépassement  : " + (elapsedMin - slaMin) + " minutes\n\n" +
                        "→ Veuillez traiter cette réclamation immédiatement.\n\n" +
                        "Plateforme de Réclamations"
        );

        mailSender.send(message);
    }
}