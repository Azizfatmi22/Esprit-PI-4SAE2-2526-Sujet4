package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.mucroservice.msenrollment.entities.InvoiceStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber; // Format: INV-2026-XXXX

    private String learnerId;

    private Long paymentId; // Référence vers la transaction de paiement

    private LocalDateTime issueDate;

    private Double totalAmount;

    private String currency;

    @ElementCollection
    @CollectionTable(name = "invoice_items", joinColumns = @JoinColumn(name = "invoice_id"))
    @Column(name = "course_title")
    private List<String> purchasedCourses;// Liste des titres des cours pour archive

    // Invoice.java
    @Column(nullable = true)
    private Long installmentPlanId;

    private String billingAddress; // Optionnel

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;// PAID, CANCELLED, etc.
    private Double discountAmount;   // ✅ réduction coupon
    private Double originalAmount;
    private String couponCode;
}