package org.example.msreportingcertification.entities;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String learnerId;

    @ManyToOne
    @JoinColumn(name = "badge_id")
    private Badge badge;

    private LocalDateTime unlockedAt = LocalDateTime.now();
    private Long evaluationHistoryId;
}
