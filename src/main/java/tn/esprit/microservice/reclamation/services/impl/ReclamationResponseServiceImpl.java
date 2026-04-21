package tn.esprit.microservice.reclamation.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationResponse;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.repositories.ReclamationRepository;
import tn.esprit.microservice.reclamation.repositories.ReclamationResponseRepository;
import tn.esprit.microservice.reclamation.services.interfaces.IReclamationResponseService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReclamationResponseServiceImpl implements IReclamationResponseService {

    @Autowired
    private ReclamationResponseRepository responseRepository;

    @Autowired
    private ReclamationRepository reclamationRepository;

    @Override
    @Transactional
    public ReclamationResponse createResponse(Long reclamationId, String learnerId,
                                              String responseText, Boolean isInternal) {
        Reclamation reclamation = reclamationRepository.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée : " + reclamationId));

        ReclamationResponse response = ReclamationResponse.builder()
                .reclamationId(reclamationId)
                .senderId(learnerId) // ou adminId
                .senderType("LEARNER") // ou ADMIN
                .responseText(responseText)
                .isInternal(isInternal != null ? isInternal : false)
                .createdDate(LocalDateTime.now())
                .build();

        ReclamationResponse saved = responseRepository.save(response);

        if (!saved.getIsInternal() && reclamation.getStatus() == ReclamationStatus.PENDING) {
            reclamation.setStatus(ReclamationStatus.IN_PROGRESS);
            reclamation.setUpdatedDate(LocalDateTime.now());
            reclamationRepository.save(reclamation);
        }

        return saved;
    }
    @Override
    public ReclamationResponse getResponseById(Long id) {
        return responseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réponse non trouvée avec l'ID : " + id));
    }

    @Override
    public List<ReclamationResponse> getResponsesByReclamationId(Long reclamationId) {
        return responseRepository.findByReclamationId(reclamationId);
    }

    @Override
    public List<ReclamationResponse> getPublicResponsesByReclamationId(Long reclamationId) {
        return responseRepository.findByReclamationIdAndIsInternalFalse(reclamationId);
    }

    @Override
    public List<ReclamationResponse> getInternalCommentsByReclamationId(Long reclamationId) {
        return responseRepository.findByReclamationIdAndIsInternalTrue(reclamationId);
    }

    @Override
    @Transactional
    public ReclamationResponse updateResponse(Long id, String responseText) {
        ReclamationResponse response = getResponseById(id);
        response.setResponseText(responseText);
        return responseRepository.save(response);
    }

    @Override
    @Transactional
    public void deleteResponse(Long id) {
        ReclamationResponse response = getResponseById(id);
        responseRepository.delete(response);
    }

    @Override
    public List<ReclamationResponse> getResponsesByLearnerId(String learnerId) {
        return responseRepository.findByLearnerId(learnerId);
    }



    @Override
    public ReclamationResponse saveResponse(ReclamationResponse response) {
        return responseRepository.save(response);
    }

}
