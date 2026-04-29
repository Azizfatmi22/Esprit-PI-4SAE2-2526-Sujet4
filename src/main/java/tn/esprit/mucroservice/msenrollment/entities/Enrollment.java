package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String learnerId;
    private Long CourseId; // ← ID statique de l'apprenant

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    private Double progress = 0.0;

    @Temporal(TemporalType.TIMESTAMP)
    private Date enrolledDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date completedDate; // null tant que non terminé

    public enum EnrollmentStatus {
        PENDING, ACTIVE, COMPLETED, CANCELLED
    }
}