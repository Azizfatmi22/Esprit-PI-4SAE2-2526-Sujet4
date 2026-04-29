package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.DTO.*;
import tn.esprit.mucroservice.msenrollment.entities.InstallmentPlan;

import java.util.List;

public interface IInstallmentService {

    // Créer un plan de paiement en plusieurs fois
    InstallmentPlanResponse createInstallmentPlan(InstallmentPlanRequest request, List<String> courseTitles);

    // Payer une échéance spécifique
    InstallmentDTO payInstallment(Long installmentId, PayInstallmentRequest request);

    // Récupérer un plan par ID
    InstallmentPlanResponse getPlanById(Long planId);

    // Récupérer tous les plans d'un apprenant
    List<InstallmentPlanResponse> getPlansByLearner(String learnerId);

    // Vérifier et marquer les échéances en retard (appelé par scheduler)
    void checkAndMarkOverdueInstallments();

    // Vérifier si un apprenant a accès à un cours (aucune échéance manquée)
    boolean hasAccessToCourse(String learnerId, Long courseId);
    void deletePlan(Long planId);
    List<InstallmentSummaryDTO> getInstallmentSummary(String learnerId);

    List<InstallmentPlanResponse> getAllPlans();
}