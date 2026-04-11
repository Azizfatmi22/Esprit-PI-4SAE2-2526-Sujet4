package org.example.msreportingcertification.controllers;

import lombok.RequiredArgsConstructor;
import org.example.msreportingcertification.entities.Badge;
import org.example.msreportingcertification.entities.UserAchievement;
import org.example.msreportingcertification.services.interfaces.IBadgeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reporting/badges")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BadgeController {

    private final IBadgeService badgeService;

    @PostMapping
    public ResponseEntity<Badge> createBadge(@RequestBody Badge badge) {
        return new ResponseEntity<>(badgeService.createBadge(badge), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Badge>> getAllBadges() {
        return ResponseEntity.ok(badgeService.getAllBadges());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Badge> getBadge(@PathVariable Long id) {
        return ResponseEntity.ok(badgeService.getBadgeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Badge> updateBadge(@PathVariable Long id, @RequestBody Badge badge) {
        return ResponseEntity.ok(badgeService.updateBadge(id, badge));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBadge(@PathVariable Long id) {
        badgeService.deleteBadge(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/achievements/{learnerId}")
    public ResponseEntity<List<UserAchievement>> getMyBadges(@PathVariable String learnerId) {
        List<UserAchievement> achievements = badgeService.getLearnerAchievements(learnerId);
        return ResponseEntity.ok(achievements);
    }
}
