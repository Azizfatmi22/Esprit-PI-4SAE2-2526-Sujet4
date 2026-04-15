package org.example.msevaluation.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LearnerExamAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String learnerId;
    private String learnerName;
    private Integer timeSpentSeconds;

    @Column(columnDefinition = "TEXT")
    private String answerOfLearner;
    private Boolean isSuspicious = false;

    private Double score;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private ExamQuestions question;

    @CreationTimestamp
    private LocalDateTime responseDate;
}
