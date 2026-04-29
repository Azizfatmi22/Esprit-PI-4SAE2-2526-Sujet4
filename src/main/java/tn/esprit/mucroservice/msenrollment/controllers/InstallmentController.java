package tn.esprit.mucroservice.msenrollment.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mucroservice.msenrollment.DTO.*;
import tn.esprit.mucroservice.msenrollment.entities.Cart;
import tn.esprit.mucroservice.msenrollment.entities.CartItem;
import tn.esprit.mucroservice.msenrollment.services.interfaces.ICartService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IInstallmentService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msenrollment/installments")
//@CrossOrigin("http://localhost:4200")
public class InstallmentController {

    @Autowired private IInstallmentService installmentService;
    @Autowired private ICartService cartService;

    // Créer un plan de paiement en plusieurs fois
    @PostMapping("/create/{learnerId}")
    public ResponseEntity<?> createPlan(
            @PathVariable String learnerId,
            @RequestBody InstallmentPlanRequest request) {
        try {
            request.setLearnerId(learnerId);

            // ✅ Récupérer les titres DEPUIS la requête si le panier est déjà vide
            List<String> courseTitles;

            Cart cart = cartService.getCartByLearnerId(learnerId);

            if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
                // Panier encore disponible → utiliser les vrais titres
                courseTitles = cart.getItems().stream()
                        .map(CartItem::getCourseTitle)
                        .collect(Collectors.toList());
            } else if (request.getCourseTitles() != null && !request.getCourseTitles().isEmpty()) {
                // Panier déjà vidé par le paiement → utiliser les titres envoyés par le frontend
                courseTitles = request.getCourseTitles();
            } else {
                // Aucun titre disponible → erreur
                return ResponseEntity.badRequest().body("Le panier est vide");
            }

            // ✅ Utiliser totalAmount de la requête si le panier est vide
            if (request.getTotalAmount() <= 0 && cart != null && cart.getItems() != null) {
                double total = cart.getItems().stream()
                        .mapToDouble(item -> item.getCoursePrice())
                        .sum();
                request.setTotalAmount(total);
            }

            InstallmentPlanResponse response =
                    installmentService.createInstallmentPlan(request, courseTitles);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Payer une échéance
    @PostMapping("/pay/{installmentId}")
    public ResponseEntity<?> payInstallment(
            @PathVariable Long installmentId,
            @RequestBody PayInstallmentRequest request) {
        try {
            InstallmentDTO result = installmentService.payInstallment(installmentId, request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Récupérer un plan par ID
    @GetMapping("/plan/{planId}")
    public ResponseEntity<InstallmentPlanResponse> getPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(installmentService.getPlanById(planId));
    }

    // Récupérer tous les plans d'un apprenant
    @GetMapping("/learner/{learnerId}")
    public ResponseEntity<List<InstallmentPlanResponse>> getLearnerPlans(
            @PathVariable String learnerId) {
        return ResponseEntity.ok(installmentService.getPlansByLearner(learnerId));
    }

    // Vérifier l'accès d'un apprenant à un cours
    @GetMapping("/access/{learnerId}/{courseId}")
    public ResponseEntity<Boolean> checkAccess(
            @PathVariable String learnerId,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(installmentService.hasAccessToCourse(learnerId, courseId));
    }
    @GetMapping("/all")
    public ResponseEntity<List<InstallmentPlanResponse>> getAllPlans() {
        return ResponseEntity.ok(installmentService.getAllPlans());
    }

    // Déclencher manuellement la vérification (utile pour les tests)
    @PostMapping("/admin/check-overdue")
    public ResponseEntity<String> triggerOverdueCheck() {
        installmentService.checkAndMarkOverdueInstallments();
        return ResponseEntity.ok("Vérification des échéances effectuée");
    }
    @DeleteMapping("/plan/{planId}")
    public ResponseEntity<String> deletePlan(@PathVariable Long planId) {
        installmentService.deletePlan(planId);
        return ResponseEntity.ok("Plan supprimé");
    }
}