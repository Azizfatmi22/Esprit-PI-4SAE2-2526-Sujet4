package org.example.msevaluation.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class QuizzQuestions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String question;

    @ManyToOne
    @JsonIgnore
    private Evaluation evaluation;

    @OneToMany(mappedBy = "quizzQuestion", cascade = CascadeType.ALL)
    private List<QuizzAnswer> quizzAnswers;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<LearnerQuizzAnswer> learnerQuizAnswers;

    @NotNull
    @Min(0)
    private Integer points;
}
