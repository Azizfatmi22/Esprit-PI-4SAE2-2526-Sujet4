package tn.esprit.microservice.reclamation.services.interfaces;

import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.entities.ReclamationType;

import java.util.List;

public interface IReclamationService {
    
    // Créer une nouvelle réclamation
    Reclamation createReclamation(Reclamation reclamation);
    
    // Récupérer une réclamation par ID
    Reclamation getReclamationById(Long id);
    
    // Récupérer toutes les réclamations
    List<Reclamation> getAllReclamations();
    
    // Récupérer les réclamations d'un apprenant
    List<Reclamation> getReclamationsByLearner(String learnerId);
    
    // Récupérer les réclamations par statut
    List<Reclamation> getReclamationsByStatus(ReclamationStatus status);
    
    // Récupérer les réclamations par type
    List<Reclamation> getReclamationsByType(ReclamationType type);
    
    // Mettre à jour le statut d'une réclamation
    Reclamation updateReclamationStatus(Long id, ReclamationStatus status, Long adminId);
    
    // Mettre à jour une réclamation
    Reclamation updateReclamation(Long id, Reclamation reclamation);
    
    // Supprimer une réclamation
    void deleteReclamation(Long id);
    
    // Récupérer les réclamations non résolues
    List<Reclamation> getUnresolvedReclamations();
    Reclamation submitSatisfaction(Long id, Integer score, String comment);

    Reclamation approveReclamation(Long id);
    Reclamation rejectReclamation(Long id);
}
