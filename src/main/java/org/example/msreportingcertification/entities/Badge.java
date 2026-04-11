package org.example.msreportingcertification.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Badge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String iconBase64;

    private Double minScoreRequired;
    private Integer maxTimeAllowed;
    private String category;

    @Enumerated(EnumType.STRING)
    private BadgeType badgeType;
    private Integer threshold;

}
