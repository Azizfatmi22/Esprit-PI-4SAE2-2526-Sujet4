package org.example.msevaluation.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LearnerQuizzAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String learnerId;
    private String learnerName;
    private Integer timeSpentSeconds;

    @ManyToOne
    private QuizzQuestions question;
    private Boolean isSuspicious = false;
    @ManyToOne
    private QuizzAnswer selectedAnswer;

    private LocalDateTime responseDate;
}