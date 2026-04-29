package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.mucroservice.msenrollment.entities.Cart;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // ✅ Une seule méthode findByLearnerId
    Optional<Cart> findByLearnerId(String learnerId);

    @Query("SELECT c FROM Cart c WHERE c.createdAt < :cutoff AND c.items IS NOT EMPTY")
    List<Cart> findCartsOlderThan(@Param("cutoff") Date cutoff);
}