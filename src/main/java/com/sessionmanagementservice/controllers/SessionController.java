package com.sessionmanagementservice.controllers;

import com.sessionmanagementservice.Services.interfaces.SessionService;
import com.sessionmanagementservice.entities.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    // ✅ Create session (with trainer from JWT)
    @PostMapping
    public ResponseEntity<Session> create(@RequestBody Session session,
                                          @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(sessionService.createSession(session, jwt));
    }

    // ✅ Get all sessions
    @GetMapping
    public ResponseEntity<List<Session>> getAll() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    // ✅ Get session by ID
    @GetMapping("/{id}")
    public ResponseEntity<Session> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    // ✅ Update session
    @PutMapping("/{id}")
    public ResponseEntity<Session> update(@PathVariable Long id,
                                          @RequestBody Session session) {
        return ResponseEntity.ok(sessionService.updateSession(id, session));
    }

    // ✅ Delete session
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelSession(@PathVariable Long id) {
        sessionService.cancelSession(id);
        return ResponseEntity.ok().build();
    }

    // ✅ Update statuses of all sessions (Planned → Ongoing → Completed)
    @PostMapping("/update-statuses")
    public ResponseEntity<Void> updateStatuses() {
        sessionService.updateSessionStatuses();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/status/update-all")
    public ResponseEntity<String> updateAllSessionsStatus() {
        sessionService.updateSessionsStatusBasedOnPlanning();
        return ResponseEntity.ok("Toutes les sessions ont été mises à jour");
    }



    // ✅ Check if trainer is available on a given date
    @GetMapping("/check-trainer-availability")
    public ResponseEntity<Boolean> isTrainerAvailable(
            @RequestParam String trainerId,
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(sessionService.isTrainerAvailable(trainerId, localDate, null));
    }

    // ✅ Check for scheduling conflict
    @GetMapping("/check-scheduling-conflict")
    public ResponseEntity<Boolean> hasSchedulingConflict(
            @RequestParam String trainerId,
            @RequestParam Long locationId,
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(sessionService.hasSchedulingConflict(trainerId, locationId, localDate, null));
    }

    // ✅ Check trainer overload
    @GetMapping("/check-trainer-overload")
    public ResponseEntity<Boolean> checkTrainerOverload(
            @RequestParam String trainerId,
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(sessionService.checkTrainerOverload(trainerId, localDate, null));
    }

    @GetMapping("/trainer")
    public ResponseEntity<List<Session>> getSessionsByTrainerId(
            @AuthenticationPrincipal Jwt jwt) {

         // or "sub" depending on your token
        return ResponseEntity.ok(sessionService.getSessionsByTrainer(jwt));
    }
}