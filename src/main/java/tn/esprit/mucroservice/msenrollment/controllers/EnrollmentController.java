package tn.esprit.mucroservice.msenrollment.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mucroservice.msenrollment.DTO.InstallmentSummaryDTO;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.services.interfaces.*;
import tn.esprit.mucroservice.msenrollment.entities.Cart;
import tn.esprit.mucroservice.msenrollment.entities.CartItem;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msenrollment")
//@CrossOrigin("http://localhost:4200")
public class EnrollmentController {

    @Autowired
    private ICartService cartService;

    @Autowired
    private IEnrollmentService enrollmentService;

    @Autowired
    private IPaymentService paymentService;

    @Autowired
    private IInvoiceService invoiceService;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private IInstallmentService installmentService;

    // --- PANIER ---

    @GetMapping("/cart/{learnerId}")
    public ResponseEntity<Cart> getCart(@PathVariable String learnerId) {
        Cart cart = cartService.getCartByLearnerId(learnerId);
        return ResponseEntity.ok(cart != null ? cart : new Cart());
    }

    @PostMapping("/cart/{learnerId}/add")
    public ResponseEntity<?> addCourseToCart(
            @PathVariable String learnerId,
            @RequestBody Map<String, Object> requestBody) {

        try {
            Long courseId = Long.valueOf(requestBody.get("courseId").toString());
            String title = (String) requestBody.get("courseTitle");
            Double price = Double.valueOf(requestBody.get("coursePrice").toString());

            cartService.addCourseToCart(learnerId, courseId, title, price);
            notificationService.notifyCourseAddedToCart(learnerId, title);

            return ResponseEntity.ok(cartService.getCartByLearnerId(learnerId));

        } catch (IllegalStateException e) {
            // ✅ Retourner 409 Conflict au lieu de 500
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @DeleteMapping("/cart/{learnerId}/remove/{itemId}")
    public ResponseEntity<String> removeItemFromCart(@PathVariable String learnerId, @PathVariable Long itemId) {
        cartService.removeItemFromCart(learnerId, itemId);
        return ResponseEntity.ok("Article supprimé");
    }

    // --- PAIEMENT ET FACTURATION (Logique complète) ---

    @PostMapping("/payment/confirm/{learnerId}")
    public ResponseEntity<?> confirmPayment(
            @PathVariable String learnerId,
            @RequestBody Map<String, Object> payload) {

        try {
            // 1. Récupérer le panier
            Cart cart = cartService.getCartByLearnerId(learnerId);
            System.out.println("Panier récupéré pour learnerId: " + learnerId);
            System.out.println("Panier: " + (cart != null ? "existe" : "null"));
            if (cart != null) {
                System.out.println("Nombre d'items: " + (cart.getItems() != null ? cart.getItems().size() : 0));
            }

            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                return ResponseEntity.badRequest().body("Le panier est vide");
            }

            // 2. Vérifier les données de paiement
            if (!payload.containsKey("amount") || !payload.containsKey("method")) {
                return ResponseEntity.badRequest().body("Données de paiement incomplètes");
            }

            Double amount;
            try {
                amount = Double.valueOf(payload.get("amount").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("Montant invalide");
            }

            // 3. Convertir la méthode de paiement
            String methodStr = payload.get("method").toString().toUpperCase();
            PaymentMethod method;
            try {
                switch (methodStr) {
                    case "CARD":     method = PaymentMethod.CARTE;     break;
                    case "FLOUCI":   method = PaymentMethod.FLOUCI;    break;
                    case "WAFA_CASH": method = PaymentMethod.WAFA_CASH; break;
                    case "BAKCHICH": method = PaymentMethod.BAKCHICH;  break;
                    case "PAYPAL":   method = PaymentMethod.PAYPAL;    break;
                    default:         method = PaymentMethod.valueOf(methodStr);
                }
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Méthode de paiement invalide: " + methodStr);
            }

            // 4. ✅ Intercepter le coupon AVANT le paiement
            Double finalAmount = amount;
            String appliedCouponCode = null;
            Double discountAmount = 0.0;

            if (payload.containsKey("couponCode")
                    && payload.get("couponCode") != null
                    && !payload.get("couponCode").toString().trim().isEmpty()) {

                String code = payload.get("couponCode").toString().trim();
                System.out.println("Tentative d'application du coupon: " + code);

                CouponValidationResult couponResult = couponService.validateOnly(code, learnerId, amount);

                if (couponResult.isValid()) {
                    finalAmount = couponResult.getFinalAmount();
                    appliedCouponCode = code.toUpperCase();
                    discountAmount = couponResult.getDiscountAmount();
                    System.out.println("✅ Coupon appliqué: " + code
                            + " | Réduction: " + discountAmount + " TND"
                            + " | Montant final: " + finalAmount + " TND");
                } else {
                    System.out.println("❌ Coupon invalide: " + couponResult.getMessage());
                    // On continue sans coupon — pas d'erreur bloquante
                }
            }

            // 5. Traiter le paiement avec finalAmount
            System.out.println("Traitement du paiement - Montant: " + finalAmount + ", Méthode: " + method);
            Payment payment = paymentService.processPayment(learnerId, finalAmount, method);
            System.out.println("Paiement créé avec ID: " + (payment != null ? payment.getId() : "null"));

            if (payment != null && payment.getId() != null) {
                try {
                    // 6. Créer les enrollments
                    System.out.println("Création des enrollments pour " + cart.getItems().size() + " cours...");
                    for (CartItem item : cart.getItems()) {
                        System.out.println("Création enrollment pour courseId: " + item.getCourseId());
                        enrollmentService.createEnrollment(learnerId, item.getCourseId());
                    }
                    System.out.println("Enrollments créés avec succès");

                    // 7. Générer la facture avec finalAmount
                    List<String> courseTitles = cart.getItems().stream()
                            .map(CartItem::getCourseTitle)
                            .collect(Collectors.toList());

                    System.out.println("Génération de la facture...");
                    Invoice invoice = invoiceService.generateInvoice(
                            learnerId,
                            payment.getId(),
                            finalAmount,   // ✅ montant après coupon
                            courseTitles,
                            appliedCouponCode,
                            discountAmount,       // ✅ 0.0 si pas de coupon
                            amount
                    );
                    System.out.println("Facture générée: " + invoice.getInvoiceNumber());

                    // 8. Vider le panier
                    System.out.println("Vidage du panier...");
                    cartService.clearCart(learnerId);
                    System.out.println("Panier vidé avec succès");

                    // 9. Construire la réponse
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Paiement réussi et facture générée");
                    response.put("invoiceNumber", invoice.getInvoiceNumber());
                    response.put("paymentStatus", "SUCCESS");
                    response.put("originalAmount", amount);
                    response.put("finalAmount", finalAmount);

                    // ✅ Infos coupon dans la réponse si appliqué
                    if (appliedCouponCode != null) {
                        response.put("couponApplied", appliedCouponCode);
                        response.put("discountAmount", discountAmount);
                        System.out.println("Coupon inclus dans la réponse: " + appliedCouponCode);
                    }

                    // 10. Notification
                    notificationService.notifyPaymentSuccess(
                            learnerId,
                            finalAmount,
                            invoice.getInvoiceNumber(),
                            payment.getId()
                    );

                    return ResponseEntity.status(HttpStatus.CREATED).body(response);

                } catch (Exception e) {
                    System.err.println("ERREUR lors des enrollments/facture: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException(
                            "Erreur lors de la création des inscriptions ou de la facture: " + e.getMessage(), e);
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Échec du paiement - Le paiement n'a pas pu être créé");

        } catch (RuntimeException e) {
            System.err.println("RuntimeException dans confirmPayment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du traitement: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception dans confirmPayment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur inattendue: " + e.getMessage());
        }
    }
    // --- CONSULTATION FACTURES ---

    @GetMapping("/invoices/all")
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/invoices/learner/{learnerId}")
    public ResponseEntity<List<Invoice>> getInvoicesByLearner(@PathVariable String learnerId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByLearner(learnerId));
    }

    @GetMapping("/invoices/number/{invoiceNumber}")
    public ResponseEntity<Invoice> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        try {
            return ResponseEntity.ok(invoiceService.getInvoiceByNumber(invoiceNumber));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // ✅ Factures des paiements directs (sans plan installment)
    // ✅ Factures des paiements directs (sans plan installment)
    @GetMapping("/invoices/direct")
    public ResponseEntity<List<Invoice>> getDirectInvoices() {
        List<Invoice> invoices = invoiceService.getAllInvoices()
                .stream()
                .filter(i -> i.getInstallmentPlanId() == null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(invoices);
    }

    // ✅ Factures des plans installment seulement
    @GetMapping("/invoices/installment")
    public ResponseEntity<List<Invoice>> getInstallmentInvoices() {
        List<Invoice> invoices = invoiceService.getAllInvoices()
                .stream()
                .filter(i -> i.getInstallmentPlanId() != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(invoices);
    }
    // --- CONSULTATION PAIEMENTS ---

    @GetMapping("/payments/all")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/payments/learner/{learnerId}")
    public ResponseEntity<List<Payment>> getPaymentsByLearner(@PathVariable String learnerId) {
        return ResponseEntity.ok(paymentService.getPaymentsByLearner(learnerId));
    }

    // Ajoutez dans EnrollmentController.java ou créez AdminEnrollmentController.java

    @GetMapping("/enrollments/all")
    public ResponseEntity<List<Enrollment>> getAllEnrollments() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }

    @GetMapping("/enrollments/learner/{learnerId}")
    public ResponseEntity<List<Enrollment>> getByLearner(@PathVariable String learnerId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByLearner(learnerId));
    }

    @PutMapping("/enrollments/{enrollmentId}/status")
    public ResponseEntity<Enrollment> updateStatus(
            @PathVariable Long enrollmentId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(enrollmentService.updateStatus(enrollmentId, status));
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<String> deleteEnrollment(@PathVariable Long enrollmentId) {
        enrollmentService.cancelEnrollment(enrollmentId);
        return ResponseEntity.ok("Enrollment annulé");
    }
    @Autowired
    private ICouponService couponService;

    // ✅ Valider un coupon (appelé depuis Angular avant paiement)
    @PostMapping("/coupons/validate")
    public ResponseEntity<CouponValidationResult> validateCoupon(
            @RequestBody Map<String, Object> body) {
        String code = body.get("code").toString();
        String learnerId = String.valueOf(body.get("learnerId").toString());
        Double amount = Double.valueOf(body.get("amount").toString());
        return ResponseEntity.ok(couponService.validateAndApply(code, learnerId, amount));
    }

    // CRUD Admin
    @GetMapping("/coupons/all")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PostMapping("/coupons/create")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.status(201).body(couponService.createCoupon(coupon));
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.updateCoupon(id, coupon));
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<String> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok("Coupon supprimé");
    }
    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<String> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok("Facture supprimée");
    }
    @DeleteMapping("/payments/{id}")
    public ResponseEntity<String> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.ok("Paiement supprimé");
    }
    // Toutes les factures d'un learner (directes + échelonnées)
    @GetMapping("/invoices/learner/{learnerId}/all")
    public ResponseEntity<List<Invoice>> getAllInvoicesByLearner(@PathVariable String learnerId) {
        return ResponseEntity.ok(invoiceService.getAllInvoicesByLearner(learnerId));
    }

    @GetMapping("/installments/learner/{learnerId}/summary")
    public ResponseEntity<List<InstallmentSummaryDTO>> getInstallmentSummary(@PathVariable String learnerId) {
        return ResponseEntity.ok(installmentService.getInstallmentSummary(learnerId));
    }
    @GetMapping("/invoices/learner/{learnerId}/direct")
    public ResponseEntity<List<Invoice>> getDirectInvoicesByLearner(@PathVariable String learnerId) {
        List<Invoice> invoices = invoiceService.getAllInvoicesByLearner(learnerId)
                .stream()
                .filter(i -> i.getInstallmentPlanId() == null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/invoices/learner/{learnerId}/installment")
    public ResponseEntity<List<Invoice>> getInstallmentInvoicesByLearner(@PathVariable String learnerId) {
        List<Invoice> invoices = invoiceService.getAllInvoicesByLearner(learnerId)
                .stream()
                .filter(i -> i.getInstallmentPlanId() != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(invoices);
    }

    // ✅ Send invoice email endpoint
    @PostMapping("/invoices/send-email")
    public ResponseEntity<?> sendInvoiceEmail(@RequestBody Map<String, Object> body) {
        try {
            String invoiceNumber = body.get("invoiceNumber").toString();
            String learnerId = body.get("learnerId").toString();

            // Get the invoice
            Invoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber);

            if (invoice == null) {
                return ResponseEntity.status(404).body("Invoice not found: " + invoiceNumber);
            }

            // Send email
            notificationService.sendInvoiceEmail(learnerId, invoice);

            return ResponseEntity.ok(Map.of(
                    "message", "Invoice email sent successfully",
                    "invoiceNumber", invoiceNumber,
                    "learnerId", learnerId
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Error sending email: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }
}

