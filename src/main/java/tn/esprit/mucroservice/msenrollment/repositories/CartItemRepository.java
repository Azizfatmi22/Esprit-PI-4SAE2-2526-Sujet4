package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.mucroservice.msenrollment.entities.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}