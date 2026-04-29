package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "bakchich_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BakchichPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_code", unique = true)
    private String paymentCode; // ex: BKC-2026-XXXX

    @Column(name = "qr_code_data")
    private String qrCodeData; // données du QR code

    @Column(name = "learner_id")
    private String learnerId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "amount")
    private Double amount;

    @Enumerated(EnumType.STRING)
    private BakchichStatus status; // PENDING_CASH, CONFIRMED, EXPIRED, CANCELLED

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt; // +24h

    @Temporal(TemporalType.TIMESTAMP)
    private Date confirmedAt;

    @Column(name = "confirmed_by")
    private String confirmedBy; // nom de l'agent qui confirme

    @Column(name = "course_titles")
    private String courseTitles;// JSON ou CSV des titres

    @Column(nullable = true)
    private Long planId;
}