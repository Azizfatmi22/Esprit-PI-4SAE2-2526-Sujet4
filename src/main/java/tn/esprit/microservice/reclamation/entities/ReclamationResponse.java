package tn.esprit.microservice.reclamation.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reclamation_responses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReclamationResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reclamation_id", nullable = false)
    private Long reclamationId;

    @Column(name = "learner_id")          // ← renommer en learnerId, type String
    private String learnerId;

    @Column(name = "response_text", nullable = false, columnDefinition = "TEXT")
    private String responseText;

    @Column(name = "is_internal", nullable = false)
    private Boolean isInternal = false;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "sender_type", nullable = false)
    private String senderType; // ADMIN ou LEARNER



    @Column(nullable = true)
    private String reaction;

    @Column(nullable = true, length = 1000)
    private String quotedText;

    @Column(nullable = true)
    private String quotedAuthor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reclamation_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private Reclamation reclamation;
}