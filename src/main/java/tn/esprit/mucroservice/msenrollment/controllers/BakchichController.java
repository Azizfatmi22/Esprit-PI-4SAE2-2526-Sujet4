package tn.esprit.mucroservice.msenrollment.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.BakchichPaymentRepository;
import tn.esprit.mucroservice.msenrollment.repositories.InvoiceRepository;
import tn.esprit.mucroservice.msenrollment.services.impl.EnrollmentServiceImpl;
import tn.esprit.mucroservice.msenrollment.services.interfaces.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msenrollment/bakchich")
public class BakchichController {

    @Autowired private IBakchichService bakchichService;
    @Autowired private ICartService cartService;
    @Autowired private IPaymentService paymentService;
    @Autowired private IInvoiceService invoiceService;
    @Autowired private INotificationService notificationService;
    @Autowired private BakchichPaymentRepository bakchichRepo;
    @Autowired
    private EnrollmentServiceImpl enrollmentServiceImpl;

    // ✅ Générer un code de paiement DIRECT (sans plan installment)
    @PostMapping("/generate/{learnerId}")
    public ResponseEntity<?> generate(
            @PathVariable String learnerId,
            @RequestBody Map<String, Object> body) {
        try {
            String phone = body.get("phoneNumber").toString();
            Double amount = Double.valueOf(body.get("amount").toString());

            Cart cart = cartService.getCartByLearnerId(learnerId);
            String titles = cart.getItems().stream()
                    .map(CartItem::getCourseTitle)
                    .collect(Collectors.joining(", "));

            BakchichPayment payment = bakchichService.generatePaymentCode(
                    learnerId, phone, amount, titles
            );

            Map<String, Object> response = new HashMap<>();
            response.put("paymentCode", payment.getPaymentCode());
            response.put("qrCodeData", payment.getQrCodeData());
            response.put("amount", payment.getAmount());
            response.put("expiresAt", payment.getExpiresAt());
            response.put("status", payment.getStatus());
            response.put("message", "Présentez ce code en agence Bakchich sous 24h");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ Générer un code de paiement lié à un plan INSTALLMENT
    @PostMapping("/generate-installment/{learnerId}")
    public ResponseEntity<?> generateInstallment(
            @PathVariable String learnerId,
            @RequestBody Map<String, Object> body) {
        try {
            String phone = body.get("phoneNumber").toString();
            Double amount = Double.valueOf(body.get("amount").toString());
            Long planId = Long.valueOf(body.get("planId").toString());
            // courseTitles optionnel
            String courseTitles = body.containsKey("courseTitles")
                    ? body.get("courseTitles").toString() : "";

            BakchichPayment payment = bakchichService.generateInstallmentCode(
                    learnerId, phone, amount, courseTitles, planId
            );

            Map<String, Object> response = new HashMap<>();
            response.put("paymentCode", payment.getPaymentCode());
            response.put("qrCodeData", payment.getQrCodeData());
            response.put("amount", payment.getAmount());
            response.put("expiresAt", payment.getExpiresAt());
            response.put("status", payment.getStatus());
            response.put("planId", payment.getPlanId());
            response.put("message", "Présentez ce code en agence Bakchich sous 24h");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ Admin — paiements directs en attente (sans plan installment)
    @GetMapping("/admin/pending")
    public ResponseEntity<List<BakchichPayment>> getPending() {
        List<BakchichPayment> pending = bakchichService.getPendingPayments()
                .stream()
                .filter(b -> b.getPlanId() == null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    // ✅ Admin — paiements installment en attente
    @GetMapping("/admin/pending/installment")
    public ResponseEntity<List<BakchichPayment>> getPendingInstallment() {
        List<BakchichPayment> pending = bakchichService.getPendingPayments()
                .stream()
                .filter(b -> b.getPlanId() != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    // ✅ Admin confirme un paiement DIRECT
    @PostMapping("/admin/confirm/{bakchichId}")
    public ResponseEntity<?> confirm(
            @PathVariable Long bakchichId,
            @RequestBody Map<String, Object> body) {
        try {
            String confirmedBy = body.getOrDefault("confirmedBy", "Agent Bakchich").toString();
            BakchichPayment bakchich = bakchichService.confirmPayment(bakchichId, confirmedBy);

            // ✅ Si c'est un Bakchich INSTALLMENT
            if (bakchich.getPlanId() != null) {
                // Créer les enrollments depuis le panier
                Cart cart = cartService.getCartByLearnerId(bakchich.getLearnerId());
                if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {

                    Payment payment = paymentService.processPayment(
                            bakchich.getLearnerId(),
                            bakchich.getAmount(),
                            PaymentMethod.BAKCHICH
                    );

                    List<String> titles = cart.getItems().stream()
                            .map(CartItem::getCourseTitle)
                            .collect(Collectors.toList());

                    Invoice invoice = invoiceService.generateInvoice(
                            bakchich.getLearnerId(),
                            payment.getId(),
                            bakchich.getAmount(),
                            titles,
                            bakchich.getPlanId()
                    );


                    // ✅ Créer les enrollments
                    for (CartItem item : cart.getItems()) {
                        enrollmentServiceImpl.createEnrollment(
                                bakchich.getLearnerId(), item.getCourseId()
                        );
                    }

                    // ✅ Vider le panier maintenant
                    cartService.clearCart(bakchich.getLearnerId());

                    // ✅ Envoyer le mail de succès maintenant
                    notificationService.notifyPaymentSuccess(
                            bakchich.getLearnerId(),
                            bakchich.getAmount(),
                            invoice.getInvoiceNumber(),
                            payment.getId()
                    );
                }

                return ResponseEntity.ok(Map.of(
                        "message", "Paiement Bakchich installment confirmé !",
                        "planId", bakchich.getPlanId(),
                        "confirmedBy", confirmedBy,
                        "status", "CONFIRMED"
                ));
            }

            // ✅ Si c'est un Bakchich DIRECT (logique existante)
            Cart cart = cartService.getCartByLearnerId(bakchich.getLearnerId());
            if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
                Payment payment = paymentService.processPayment(
                        bakchich.getLearnerId(), bakchich.getAmount(), PaymentMethod.BAKCHICH
                );
                List<String> titles = cart.getItems().stream()
                        .map(CartItem::getCourseTitle).collect(Collectors.toList());
                Invoice invoice = invoiceService.generateInvoice(
                        bakchich.getLearnerId(), payment.getId(), bakchich.getAmount(), titles
                );
                cartService.clearCart(bakchich.getLearnerId());
                notificationService.notifyPaymentSuccess(
                        bakchich.getLearnerId(), bakchich.getAmount(),
                        invoice.getInvoiceNumber(), payment.getId()
                );
                return ResponseEntity.ok(Map.of(
                        "message", "Paiement Bakchich confirmé !",
                        "invoiceNumber", invoice.getInvoiceNumber(),
                        "status", "CONFIRMED"
                ));
            }

            return ResponseEntity.ok(Map.of("message", "Confirmé", "status", "CONFIRMED"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ Annuler un paiement
    @PostMapping("/admin/cancel/{bakchichId}")
    public ResponseEntity<?> cancel(@PathVariable Long bakchichId) {
        try {
            bakchichService.cancelPayment(bakchichId);
            return ResponseEntity.ok(Map.of("message", "Paiement annulé"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ Historique des paiements d'un learner
    @GetMapping("/learner/{learnerId}")
    public ResponseEntity<?> getByLearner(@PathVariable String learnerId) {
        return ResponseEntity.ok(bakchichService.getPaymentsByLearner(learnerId));
    }
}