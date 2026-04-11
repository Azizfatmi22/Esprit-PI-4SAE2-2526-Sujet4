package com.mspathandschedule.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String notes;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;

    @Enumerated(EnumType.STRING)
    private ScheduleType type;

    private Long planningId; // Reference to Planning in MS-PlanningManagement
}