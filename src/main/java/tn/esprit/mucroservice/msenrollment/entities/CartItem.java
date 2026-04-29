package tn.esprit.mucroservice.msenrollment.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

// CartItem.java
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    @JsonBackReference // Critical to stop the 500 error
    private Cart cart;

    private Long courseId;
    private String courseTitle;
    private Double coursePrice;
}