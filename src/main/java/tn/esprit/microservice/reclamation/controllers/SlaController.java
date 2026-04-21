package tn.esprit.microservice.reclamation.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.microservice.reclamation.entities.SlaAlert;
import tn.esprit.microservice.reclamation.repositories.SlaAlertRepository;
import tn.esprit.microservice.reclamation.services.impl.SlaService;

import java.util.List;

@RestController
@RequestMapping("/msreclamation/sla")
@RequiredArgsConstructor
public class SlaController {

    private final SlaService slaService;
    private final SlaAlertRepository slaAlertRepository;

    // Toutes les alertes actives
    @GetMapping("/alerts")
    public ResponseEntity<List<SlaAlert>> getActiveAlerts() {
        return ResponseEntity.ok(slaService.getActiveAlerts());
    }

    // Alertes d'une réclamation spécifique
    @GetMapping("/alerts/reclamation/{id}")
    public ResponseEntity<List<SlaAlert>> getAlertsByReclamation(@PathVariable Long id) {
        return ResponseEntity.ok(
                slaAlertRepository.findByReclamationIdOrderByAlertDateDesc(id)
        );
    }

    // Résoudre manuellement une alerte
    @PutMapping("/alerts/{reclamationId}/resolve")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long reclamationId) {
        slaService.resolveAlert(reclamationId);
        return ResponseEntity.ok().build();
    }
}