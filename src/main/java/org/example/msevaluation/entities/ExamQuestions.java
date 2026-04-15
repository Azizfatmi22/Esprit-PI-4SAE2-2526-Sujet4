package org.example.msevaluation.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ExamQuestions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String question;



    @NotBlank(message = "La correction de référence est obligatoire pour le formateur")
    @Column(columnDefinition = "TEXT")
    private String trainerCorrection;

    @NotNull
    @Min(0)
    private Integer points;

    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    private Evaluation evaluation;

    @ElementCollection
    @CollectionTable(name = "exam_question_keywords", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "keyword")
    private List<String> keywords = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // On ne veut pas charger les réponses quand on crée l'examen
    private List<LearnerExamAnswer> learnerAnswers;

}
