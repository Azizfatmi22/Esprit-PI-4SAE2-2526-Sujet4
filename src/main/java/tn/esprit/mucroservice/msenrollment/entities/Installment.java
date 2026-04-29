package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "installments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_plan_id", nullable = false)
    private InstallmentPlan installmentPlan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber; // 1, 2, 3...

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Temporal(TemporalType.DATE)
    @Column(name = "due_date", nullable = false)
    private Date dueDate; // date limite de paiement

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "paid_date")
    private Date paidDate; // date réelle de paiement

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @Column(name = "invoice_number")
    private String invoiceNumber; // facture générée pour cette échéance

    @Column(name = "transaction_id")
    private String transactionId;
}