package tn.esprit.microservice.reclamation.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.microservice.reclamation.entities.SlaAlert;

import java.util.List;
import java.util.Optional;

public interface SlaAlertRepository extends JpaRepository<SlaAlert, Long> {

    // Vérifie si une alerte existe déjà pour cette réclamation
    Optional<SlaAlert> findByReclamationIdAndResolvedFalse(Long reclamationId);

    // Toutes les alertes actives (non résolues)
    List<SlaAlert> findByResolvedFalseOrderByAlertDateDesc();

    // Alertes d'une réclamation
    List<SlaAlert> findByReclamationIdOrderByAlertDateDesc(Long reclamationId);
}