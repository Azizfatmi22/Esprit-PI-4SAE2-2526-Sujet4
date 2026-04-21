package tn.esprit.microservice.reclamation.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.repositories.ReclamationRepository;
import tn.esprit.microservice.reclamation.services.impl.SlaService;


import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReclamationScheduler {

    private final ReclamationRepository reclamationRepository;
    private final SlaService slaService;

    // ── Toutes les 2 minutes : vérifie les SLA ────────────────────────────────
    @Scheduled(fixedRate = 120000)
    public void checkSlaBreaches() {
        System.out.println("🔍 Vérification SLA...");

        reclamationRepository.findAll().forEach(r -> {
            if (slaService.isSlaBreached(r)) {
                slaService.createAlertIfNotExists(r);
                System.out.println("🚨 SLA dépassé: réclamation #" + r.getId());
            }
        });
    }

    // ── Toutes les 5 minutes : gestion auto des statuts ───────────────────────
    @Scheduled(fixedRate = 300000)
    public void autoManageReclamations() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);

        reclamationRepository.findAll().forEach(r -> {

            // IN_PROGRESS sans activité → RESOLVED
            if (r.getStatus() == ReclamationStatus.IN_PROGRESS) {
                LocalDateTime date = r.getUpdatedDate() != null
                        ? r.getUpdatedDate() : r.getCreatedDate();
                if (date != null && date.isBefore(tenMinutesAgo)) {
                    r.setStatus(ReclamationStatus.RESOLVED);
                    r.setResolvedDate(LocalDateTime.now());
                    r.setUpdatedDate(LocalDateTime.now());
                    reclamationRepository.save(r);
                    // Résoudre l'alerte SLA
                    slaService.resolveAlert(r.getId());
                    System.out.println("✅ Auto-resolved: #" + r.getId());
                }
            }

            // RESOLVED → CLOSED après 10 min
            if (r.getStatus() == ReclamationStatus.RESOLVED) {
                LocalDateTime date = r.getResolvedDate() != null
                        ? r.getResolvedDate() : r.getUpdatedDate();
                if (date != null && date.isBefore(tenMinutesAgo)) {
                    r.setStatus(ReclamationStatus.CLOSED);
                    r.setUpdatedDate(LocalDateTime.now());
                    reclamationRepository.save(r);
                    slaService.resolveAlert(r.getId());
                    System.out.println("✅ Auto-closed: #" + r.getId());
                }
            }
        });
    }
}