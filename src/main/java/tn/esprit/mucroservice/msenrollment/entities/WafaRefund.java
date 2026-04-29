package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "wafa_refunds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WafaRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "learner_id")
    private String learnerId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(name = "payment_id")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    private RefundStatus status; // PENDING, PROCESSED, REJECTED

    @Temporal(TemporalType.TIMESTAMP)
    private Date requestedAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date processedAt;
}