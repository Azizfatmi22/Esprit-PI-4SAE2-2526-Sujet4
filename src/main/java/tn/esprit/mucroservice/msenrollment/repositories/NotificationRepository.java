package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.mucroservice.msenrollment.entities.Notification;
import tn.esprit.mucroservice.msenrollment.entities.NotificationType;

import java.util.Date;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByLearnerIdOrderByCreatedAtDesc(String learnerId);
    List<Notification> findByLearnerIdAndIsReadFalse(String learnerId);
    long countByLearnerIdAndIsReadFalse(String learnerId);

    // Notifications non encore envoyées par email
    List<Notification> findByEmailSentFalse();

    // Éviter les doublons — vérifier si notification existe déjà
    boolean existsByLearnerIdAndTypeAndRelatedId(String learnerId, NotificationType type, Long relatedId);

    // Paniers créés avant une certaine date sans notification
    @Query("SELECT n FROM Notification n WHERE n.type = 'CART_ABANDONED' AND n.createdAt < :cutoff")
    List<Notification> findAbandonedCartNotificationsBefore(Date cutoff);
}