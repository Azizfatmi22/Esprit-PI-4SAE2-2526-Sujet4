package com.example.mstrainerhiring.entities;

import com.example.mstrainerhiring.enums.City;
import com.example.mstrainerhiring.enums.Technology;
import com.example.mstrainerhiring.enums.TrainerStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trainer_hiring")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerHiring {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Name is required")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Forename is required")
    @Column(name = "forename", nullable = false)
    private String forename;

    @NotNull(message = "Location is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "location", nullable = false, columnDefinition = "VARCHAR(255)")
    private City location;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(name = "email", nullable = false)
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[2579][0-9]{7}$", message = "Phone must be a valid Tunisian number")
    @Column(name = "phone", nullable = false)
    private String phone;

    @NotBlank(message = "Motivation letter is required")
    @Column(name = "motivation_letter", nullable = false, columnDefinition = "TEXT")
    private String motivationLetter;

    @NotNull(message = "Partner selection is required")
    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "years_of_experience", nullable = true)
    private Integer yearsOfExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "technology", nullable = true, columnDefinition = "VARCHAR(255)")
    private Technology technology;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(255)")
    @Builder.Default
    private TrainerStatus status = TrainerStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "trainer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TrainerDocument> documents = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    // Intelligence Scores
    @Column(name = "skill_sync_score")
    private Double skillSyncScore;

    @Column(name = "plagiarism_flag")
    private Boolean plagiarismFlag;

    @Column(name = "tone_clarity_score")
    private Double toneClarityScore;

    @Column(name = "acceptance_probability")
    private Double acceptanceProbability;

    @Column(name = "intelligent_analysis_context", columnDefinition = "TEXT")
    private String intelligentAnalysisContext;

    @Column(name = "is_blank_cv")
    private Boolean isBlankCv;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
