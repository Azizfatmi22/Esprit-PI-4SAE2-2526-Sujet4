package com.sessionmanagementservice.controllers;

import com.sessionmanagementservice.Services.interfaces.PlanningService;
import com.sessionmanagementservice.entities.Planning;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plannings")
@CrossOrigin(origins = "http://localhost:4200")
public class PlanningController {

    private final PlanningService planningService;

    PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping
    public ResponseEntity<Planning> create(
            @RequestParam int sessionId,
            @RequestParam Long locationId,
            @RequestBody Planning planning) {

        return ResponseEntity.ok(
                planningService.createPlanning(planning, sessionId, locationId)
        );
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Planning>> getBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(planningService.getPlanningsBySession(sessionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Planning> getById(@PathVariable int id) {
        return ResponseEntity.ok(planningService.getPlanningById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Planning> update(@PathVariable int id,
                                           @RequestBody Planning planning) {
        return ResponseEntity.ok(planningService.updatePlanning(id, planning));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        planningService.deletePlanning(id);
        return ResponseEntity.noContent().build();
    }
}
