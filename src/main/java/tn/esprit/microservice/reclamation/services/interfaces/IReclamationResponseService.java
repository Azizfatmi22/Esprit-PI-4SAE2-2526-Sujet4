package tn.esprit.microservice.reclamation.services.interfaces;

import tn.esprit.microservice.reclamation.entities.ReclamationResponse;

import java.util.List;

public interface IReclamationResponseService {

    // Créer une réponse à une réclamation
    ReclamationResponse createResponse(Long reclamationId, String learnerId,
                                       String responseText, Boolean isInternal);

    // Récupérer une réponse par ID
    ReclamationResponse getResponseById(Long id);

    // Récupérer toutes les réponses d'une réclamation
    List<ReclamationResponse> getResponsesByReclamationId(Long reclamationId);

    // Récupérer les réponses publiques (visibles par l'apprenant)
    List<ReclamationResponse> getPublicResponsesByReclamationId(Long reclamationId);

    // Récupérer les commentaires internes
    List<ReclamationResponse> getInternalCommentsByReclamationId(Long reclamationId);

    // Mettre à jour une réponse
    ReclamationResponse updateResponse(Long id, String responseText);

    // Supprimer une réponse
    void deleteResponse(Long id);

    // Récupérer les réponses d'un admin
    List<ReclamationResponse> getResponsesByLearnerId(String learnerId);

    ReclamationResponse saveResponse(ReclamationResponse response);
}
