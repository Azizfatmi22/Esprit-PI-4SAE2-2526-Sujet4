package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "installment_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "learner_id", nullable = false)
    private String learnerId;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "number_of_installments", nullable = false)
    private Integer numberOfInstallments; // 3 ou 6

    @Column(name = "fee_percentage", nullable = false)
    private Double feePercentage; // 0.0 pour 3x, 5.0 pour 6x

    @Column(name = "amount_with_fees", nullable = false)
    private Double amountWithFees; // montant total après frais

    @Column(name = "installment_amount", nullable = false)
    private Double installmentAmount; // montant par échéance

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InstallmentPlanStatus status = InstallmentPlanStatus.ACTIVE;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    public Date getCreatedAt() {
        return createdAt;
    }

    @OneToMany(mappedBy = "installmentPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Installment> installments;
}