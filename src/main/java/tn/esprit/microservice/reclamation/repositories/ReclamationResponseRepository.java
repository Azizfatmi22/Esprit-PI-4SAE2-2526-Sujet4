package tn.esprit.microservice.reclamation.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.microservice.reclamation.entities.ReclamationResponse;

import java.util.List;

@Repository
public interface ReclamationResponseRepository extends JpaRepository<ReclamationResponse, Long> {
    
    // Trouver toutes les réponses d'une réclamation
    List<ReclamationResponse> findByReclamationId(Long reclamationId);
    
    // Trouver les réponses publiques (visibles par l'apprenant)
    List<ReclamationResponse> findByReclamationIdAndIsInternalFalse(Long reclamationId);
    
    // Trouver les commentaires internes
    List<ReclamationResponse> findByReclamationIdAndIsInternalTrue(Long reclamationId);
    
    // Trouver les réponses d'un admin spécifique
    List<ReclamationResponse> findByLearnerId(String learnerId);
    
    // Compter les réponses d'une réclamation
    long countByReclamationId(Long reclamationId);
}
