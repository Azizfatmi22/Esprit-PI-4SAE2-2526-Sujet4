package com.sessionmanagementservice.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    private Integer maxParticipants;

    private LocalDate createdAt;

    @OneToOne
    @JoinColumn(name = "session_id")
    private Session session;
}
