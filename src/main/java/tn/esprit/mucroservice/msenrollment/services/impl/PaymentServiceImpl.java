package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.PaymentRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.ICartService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IEnrollmentService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IPaymentService;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements IPaymentService {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ICartService cartService;
    @Autowired private IEnrollmentService enrollmentService;


    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public List<Payment> getPaymentsByLearner(String learnerId) {
        return paymentRepository.findByLearnerId(learnerId);
    }

    @Transactional
    @Override
    public Payment processPayment(String learnerId, Double amount, PaymentMethod method) {
        try {
            System.out.println("PaymentServiceImpl - Début du traitement du paiement");
            System.out.println("LearnerId: " + learnerId + ", Amount: " + amount + ", Method: " + method);
            
            // 1. Get the current cart
            Cart cart = cartService.getCartByLearnerId(learnerId);
            System.out.println("Cart récupéré: " + (cart != null ? "OUI" : "NON"));
            
            if (cart == null) {
                System.err.println("ERREUR: Le panier est null");
                throw new RuntimeException("Le panier est null");
            }
            
            if (cart.getItems() == null) {
                System.err.println("ERREUR: Les items du panier sont null");
                throw new RuntimeException("Les items du panier sont null");
            }
            
            if (cart.getItems().isEmpty()) {
                System.err.println("ERREUR: Le panier est vide");
                throw new RuntimeException("Le panier est vide");
            }
            
            System.out.println("Nombre d'items dans le panier: " + cart.getItems().size());

            // 2. Create and Save Payment
            Payment payment = new Payment();
            payment.setLearnerId(learnerId);
            payment.setAmount(amount);
            payment.setMethod(method);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(UUID.randomUUID().toString());
            
            System.out.println("Sauvegarde du paiement...");
            payment = paymentRepository.save(payment);
            System.out.println("Paiement sauvegardé avec ID: " + payment.getId());

            // Note: Les enrollments et le vidage du panier sont gérés dans le controller
            // pour éviter les doublons et assurer la cohérence transactionnelle

            return payment;
        } catch (Exception e) {
            System.err.println("ERREUR dans processPayment: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du traitement du paiement: " + e.getMessage(), e);
        }
    }
    @Override
    public void deletePayment(Long id) {
        paymentRepository.deleteById(id);
    }
}