package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String learnerId;
    private Long invoiceId;
    private Long paymentId;

    private String reason;
    private Double refundAmount;

    @Enumerated(EnumType.STRING)
    private RefundStatus status; // PENDING, APPROVED, REJECTED

    private LocalDateTime requestDate;
    private LocalDateTime processedDate;
    private String processedBy; // admin username

    private String rejectionReason;
    private String creditNoteNumber; // numéro de l'avoir
}