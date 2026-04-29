package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // ex: "WELCOME20", "PYTHON50"

    private String description;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType; // PERCENTAGE ou FIXED

    private Double discountValue; // 20.0 = 20% ou 50.0 = 50 TND

    private Double minOrderAmount; // montant minimum pour utiliser le coupon

    private Double maxDiscountAmount; // plafond de réduction (ex: max 100 TND)

    private Integer maxUsages; // null = illimité

    private Integer currentUsages;

    private String learnerId; // null = pour tous, sinon coupon personnel

    private String category; // null = tous les cours, sinon "PYTHON", "WEB", etc.

    private LocalDateTime validUntil; // date d'expiration

    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    private CouponType type; // GENERAL, WELCOME, CATEGORY
}