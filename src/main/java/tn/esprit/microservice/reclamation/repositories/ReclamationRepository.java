package tn.esprit.microservice.reclamation.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.entities.ReclamationType;

import java.util.List;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    
    // Trouver toutes les réclamations d'un apprenant
    List<Reclamation> findByLearnerId(String learnerId);
    
    // Trouver les réclamations par statut
    List<Reclamation> findByStatus(ReclamationStatus status);
    
    // Trouver les réclamations par type
    List<Reclamation> findByType(ReclamationType type);
    
    // Trouver les réclamations d'un apprenant par statut
    List<Reclamation> findByLearnerIdAndStatus(String learnerId, ReclamationStatus status);
    
    // Trouver les réclamations liées à un cours
    List<Reclamation> findByCourseId(Long courseId);
    
    // Compter les réclamations par statut
    long countByStatus(ReclamationStatus status);
    
    // Trouver les réclamations non résolues
    List<Reclamation> findByStatusNot(ReclamationStatus status);

}
