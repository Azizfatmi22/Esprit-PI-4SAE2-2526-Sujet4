package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "flouci_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlouciTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "learner_id", nullable = false)
    private String learnerId;

    @Column(name = "transaction_ref", unique = true)
    private String transactionRef; // ex: FLC-2026-XXXX

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "otp_code")
    private String otpCode; // stocké temporairement

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry; // expire dans 2 min

    @Enumerated(EnumType.STRING)
    private FlouciStatus status; // PENDING_OTP, VERIFIED, FAILED, EXPIRED

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
}
