package tn.esprit.microservice.reclamation.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reclamations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Champs communs ────────────────────────────────────────
    @Column(name = "learner_id", nullable = false)
    private String learnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ReclamationType type;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReclamationStatus status = ReclamationStatus.PENDING;

    @Column(name = "priority")
    private Integer priority = 3;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;           // Commun optionnel

    @Column(name = "desired_resolution_date")
    private LocalDate desiredResolutionDate; // Commun optionnel

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;          // Commun optionnel

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;         // Commun optionnel

    // ── TECHNICAL ─────────────────────────────────────────────
    @Column(name = "browser_name", length = 100)
    private String browserName;            // Ex: Chrome 120, Firefox

    @Column(name = "os_version", length = 100)
    private String osVersion;              // Ex: Windows 11, macOS 14

    @Column(name = "error_code", length = 50)
    private String errorCode;             // Ex: 500, ERR_CONNECTION

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;           // Message d'erreur exact

    // ── PAYMENT ───────────────────────────────────────────────
    @Column(name = "transaction_id", length = 100)
    private String transactionId;          // Référence transaction

    @Column(name = "payment_date")
    private LocalDate paymentDate;     // Date du paiement

    @Column(name = "amount")
    private Double amount;                 // Montant concerné

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;          // CB, Virement, PayPal...

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;          // Numéro de facture

    // ── CONTENT ───────────────────────────────────────────────
    @Column(name = "course_id")
    private Long courseId;                 // Cours concerné

    @Column(name = "lesson_id")
    private Long lessonId;                 // Leçon précise

    @Column(name = "content_type", length = 50)
    private String contentType;            // Vidéo, Quiz, PDF...

    @Column(name = "page_url", length = 500)
    private String pageUrl;                // URL de la page problème

    // ── ACCESS ────────────────────────────────────────────────
    @Column(name = "access_date")
    private LocalDate accessDate;      // Quand l'accès a échoué

    @Column(name = "device_type", length = 50)
    private String deviceType;             // Mobile, PC, Tablette

    // ── CERTIFICATE ───────────────────────────────────────────
    @Column(name = "completion_date")
    private LocalDate completionDate;  // Date de fin du cours

    @Column(name = "certificate_type", length = 100)
    private String certificateType;        // Type de certificat

    @Column(name = "satisfaction_score")
    private Integer satisfactionScore;

    @Column(name = "satisfaction_comment", columnDefinition = "TEXT")
    private String satisfactionComment;

    @Column(name = "satisfaction_date")
    private LocalDateTime satisfactionDate;

    // Dans Reclamation.java - Ajouter ces champs
    @Column(name = "external_ticket_id", length = 100)
    private String externalTicketId;

    @Column(name = "external_ticket_url", length = 500)
    private String externalTicketUrl;

    @Column(name = "external_tool", length = 50)
    private String externalTool;

    // Dans Reclamation.java - Ajouter
    @Column(name = "is_suspect")
    private Boolean isSuspect = false;

    @Column(name = "moderation_reason", columnDefinition = "TEXT")
    private String moderationReason;

    @Column(name = "moderated_by")
    private Long moderatedBy;

    @Column(name = "moderated_date")
    private LocalDateTime moderatedDate;

    @Column(name = "gravity_score")
    private Integer gravityScore;  // 1-10

    @Column(name = "gravity_level")
    private String gravityLevel;   // "CRITICAL", "HIGH", "MEDIUM", "LOW"

    @OneToMany(mappedBy = "reclamation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ReclamationResponse> responses;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
        this.status = ReclamationStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = LocalDateTime.now();
        if (this.status == ReclamationStatus.RESOLVED && this.resolvedDate == null) {
            this.resolvedDate = LocalDateTime.now();
        }
    }
}