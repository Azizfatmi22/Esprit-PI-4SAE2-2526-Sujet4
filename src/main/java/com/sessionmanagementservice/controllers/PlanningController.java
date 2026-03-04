package com.sessionmanagementservice.controllers;

import com.sessionmanagementservice.Services.interfaces.PlanningService;
import com.sessionmanagementservice.entities.Planning;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plannings")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    // ==================== CRUD BASIQUE ====================

    @PostMapping
    public ResponseEntity<Planning> create(
            @RequestParam Long sessionId,
            @RequestParam(required = false) Long locationId,
            @RequestBody Planning planning) {

        return ResponseEntity.ok(
                planningService.createPlanning(planning, sessionId, locationId)
        );
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Planning>> getBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(
                planningService.getPlanningsBySession(sessionId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Planning> getById(@PathVariable int id) {
        return ResponseEntity.ok(
                planningService.getPlanningById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Planning> update(
            @PathVariable int id,
            @RequestBody Planning planning) {

        return ResponseEntity.ok(
                planningService.updatePlanning(id, planning)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        planningService.deletePlanning(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== FONCTIONS AVANCÉES ====================
    // ✅ Ces méthodes retournent maintenant un seul Planning

    @PostMapping("/generate")
    public ResponseEntity<Planning> generatePlanning(  // ✅ Retourne Planning (plus List)
                                                       @RequestParam Long sessionId) {

        return ResponseEntity.ok(
                planningService.generatePlanning(sessionId)
        );
    }

    @PostMapping("/distribute")
    public ResponseEntity<Planning> distributePlanning(  // ✅ Retourne Planning (plus List)
                                                         @RequestParam Long sessionId,
                                                         @RequestParam Long locationId,
                                                         @RequestParam int numberOfDays) {

        return ResponseEntity.ok(
                planningService.distributePlanning(sessionId, locationId, numberOfDays)
        );
    }

    @GetMapping("/conflict")
    public ResponseEntity<Boolean> hasConflict(
            @RequestParam Long locationId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        return ResponseEntity.ok(
                planningService.hasPlanningConflict(
                        locationId,
                        LocalDate.parse(startDate),
                        LocalDate.parse(endDate)
                )
        );
    }

    @GetMapping("/suggest-date")
    public ResponseEntity<LocalDate> suggestDate(
            @RequestParam Long locationId,
            @RequestParam String startDate) {

        return ResponseEntity.ok(
                planningService.suggestNextAvailableDate(
                        locationId,
                        LocalDate.parse(startDate)
                )
        );
    }

    @PostMapping("/optimize")
    public ResponseEntity<Planning> optimizePlanning(  // ✅ Retourne Planning (plus List)
                                                       @RequestParam Long sessionId) {

        return ResponseEntity.ok(
                planningService.optimizePlanning(sessionId)
        );
    }

    @GetMapping("/count-by-location")
    public ResponseEntity<Long> countByLocation(
            @RequestParam Long locationId) {

        return ResponseEntity.ok(
                planningService.countPlanningsByLocation(locationId)
        );
    }

    @PostMapping("/fill-gaps")
    public ResponseEntity<Planning> fillGaps(  // ✅ Retourne Planning (plus List)
                                               @RequestParam Long sessionId,
                                               @RequestParam Long locationId) {

        return ResponseEntity.ok(
                planningService.fillGaps(sessionId, locationId)
        );
    }

    @PostMapping("/rolling")
    public ResponseEntity<Planning> maintainRollingPlanning(  // ✅ Retourne Planning (plus List)
                                                              @RequestParam Long sessionId,
                                                              @RequestParam Long locationId,
                                                              @RequestParam int daysAhead) {

        return ResponseEntity.ok(
                planningService.maintainRollingPlanning(sessionId, locationId, daysAhead)
        );
    }

    @GetMapping("/best-location")
    public ResponseEntity<?> suggestBestLocation(
            @RequestParam String date) {

        return ResponseEntity.ok(
                planningService.suggestBestLocation(LocalDate.parse(date))
        );
    }

    @GetMapping("/busy-days")
    public ResponseEntity<Map<String, Object>> getBusyDays(
            @RequestParam Long locationId) {

        return ResponseEntity.ok(
                planningService.getBusyDays(locationId)
        );
    }

    @GetMapping("/high-risk")
    public ResponseEntity<Map<String, Object>> isHighRisk(
            @RequestParam Long sessionId) {

        return ResponseEntity.ok(
                planningService.isHighRiskPlanning(sessionId)
        );
    }

    @GetMapping("/smart-date")
    public ResponseEntity<LocalDate> smartSuggestDate(
            @RequestParam Long locationId,
            @RequestParam String startDate) {

        return ResponseEntity.ok(
                planningService.smartSuggestDate(
                        locationId,
                        LocalDate.parse(startDate)
                )
        );
    }
}