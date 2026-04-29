package tn.esprit.mucroservice.msenrollment.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mucroservice.msenrollment.entities.Notification;
import tn.esprit.mucroservice.msenrollment.services.interfaces.INotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msenrollment/notifications")
//@CrossOrigin("http://localhost:4200")
public class NotificationController {

    @Autowired private INotificationService notificationService;

    @GetMapping("/learner/{learnerId}")
    public ResponseEntity<List<Notification>> getAll(@PathVariable String learnerId) {
        return ResponseEntity.ok(notificationService.getNotificationsByLearner(learnerId));
    }

    @GetMapping("/learner/{learnerId}/unread")
    public ResponseEntity<List<Notification>> getUnread(@PathVariable String learnerId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(learnerId));
    }

    @GetMapping("/learner/{learnerId}/count")
    public ResponseEntity<Map<String, Long>> getCount(@PathVariable String learnerId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(learnerId)));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/learner/{learnerId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String learnerId) {
        notificationService.markAllAsRead(learnerId);
        return ResponseEntity.ok().build();
    }
}