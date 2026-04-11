package com.mspathandschedule.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private LearningLevel level;

    @Enumerated(EnumType.STRING)
    private LearningPathStatus status;

    private Integer totalHours;

    private String objectives;

    // Reference to sessions in MS-SessionManagement
    @ElementCollection
    @CollectionTable(name = "learning_path_sessions", joinColumns = @JoinColumn(name = "learning_path_id"))
    @Column(name = "session_id")
    private List<Long> sessionIds;
}