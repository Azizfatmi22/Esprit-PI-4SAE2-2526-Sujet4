package com.sessionmanagementservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Session {




        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;


        private Long courseId;
        private Long trainerId;

        @Enumerated(EnumType.STRING)
        private SessionStatus status;

        private Integer maxParticipants;

        private LocalDate createdAt;

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public SessionStatus getStatus() {
                return status;
        }

        public void setStatus(SessionStatus status) {
                this.status = status;
        }

        public Integer getMaxParticipants() {
                return maxParticipants;
        }

        public void setMaxParticipants(Integer maxParticipants) {
                this.maxParticipants = maxParticipants;
        }

        public LocalDate getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(LocalDate createdAt) {
                this.createdAt = createdAt;
        }

        public Planning getPlanning() {
                return planning;
        }

        public void setPlanning(Planning planning) {
                this.planning = planning;
        }

        public Attendance getAttendance() {
                return attendance;
        }

        public void setAttendance(Attendance attendance) {
                this.attendance = attendance;
        }

        @OneToOne(mappedBy = "session", cascade = CascadeType.ALL)
        @JsonIgnore
        private Planning planning;

        @OneToOne(mappedBy = "session", cascade = CascadeType.ALL)
        private Attendance attendance;

        @PrePersist
        public void onCreate() {
            createdAt = LocalDate.now();
            status = SessionStatus.PLANNED;
        }
    }


