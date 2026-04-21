package tn.esprit.microservice.reclamation.services.interfaces;


import tn.esprit.microservice.reclamation.entities.ModerationResult;
import tn.esprit.microservice.reclamation.entities.Reclamation;

public interface IModerationService {
    ModerationResult scanReclamation(Reclamation reclamation);
}