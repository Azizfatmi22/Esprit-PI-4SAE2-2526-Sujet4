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

    // ✅ CREATE
    @PostMapping
    public ResponseEntity<Planning> create(
            @RequestParam Long sessionId,
            @RequestParam(required = false) Long locationId,
            @RequestBody Planning planning) {

        return ResponseEntity.ok(
                planningService.createPlanning(planning, sessionId, locationId)
        );
    }

    // ✅ GET BY SESSION
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Planning>> getBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(
                planningService.getPlanningsBySession(sessionId)
        );
    }

    // ✅ GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Planning> getById(@PathVariable int id) {
        return ResponseEntity.ok(
                planningService.getPlanningById(id)
        );
    }

    // ✅ UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Planning> update(
            @PathVariable int id,
            @RequestBody Planning planning) {

        return ResponseEntity.ok(
                planningService.updatePlanning(id, planning)
        );
    }

    // ✅ DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        planningService.deletePlanning(id);
        return ResponseEntity.noContent().build();
    }

    // =====================================================
    // 🔥 ADVANCED FUNCTIONS (MATCHED EXACTLY)
    // =====================================================

    // ✅ Generate planning automatically
    @PostMapping("/generate")
    public ResponseEntity<List<Planning>> generatePlanning(
            @RequestParam Long sessionId,
            @RequestParam Long locationId) {

        return ResponseEntity.ok(
                planningService.generatePlanning(sessionId, locationId)
        );
    }

    // ✅ Distribute planning across days
    @PostMapping("/distribute")
    public ResponseEntity<List<Planning>> distributePlanning(
            @RequestParam Long sessionId,
            @RequestParam Long locationId,
            @RequestParam int numberOfDays) {

        return ResponseEntity.ok(
                planningService.distributePlanning(sessionId, locationId, numberOfDays)
        );
    }

    // ✅ Check conflict
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

    // ✅ Suggest next available date
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

    // ✅ Optimize planning
    @PostMapping("/optimize")
    public ResponseEntity<List<Planning>> optimizePlanning(
            @RequestParam Long sessionId) {

        return ResponseEntity.ok(
                planningService.optimizePlanning(sessionId)
        );
    }

    // ✅ Count plannings by location
    @GetMapping("/count-by-location")
    public ResponseEntity<Long> countByLocation(
            @RequestParam Long locationId) {

        return ResponseEntity.ok(
                planningService.countPlanningsByLocation(locationId)
        );
    }

    // ✅ Fill gaps between sessions
    @PostMapping("/fill-gaps")
    public ResponseEntity<List<Planning>> fillGaps(
            @RequestParam Long sessionId,
            @RequestParam Long locationId) {

        return ResponseEntity.ok(
                planningService.fillGaps(sessionId, locationId)
        );
    }

    // ✅ Maintain rolling planning
    @PostMapping("/rolling")
    public ResponseEntity<List<Planning>> maintainRollingPlanning(
            @RequestParam Long sessionId,
            @RequestParam Long locationId,
            @RequestParam int daysAhead) {

        return ResponseEntity.ok(
                planningService.maintainRollingPlanning(sessionId, locationId, daysAhead)
        );
    }
    // ✅ Suggest best location (least busy)
    @GetMapping("/best-location")
    public ResponseEntity<?> suggestBestLocation(
            @RequestParam String date) {

        return ResponseEntity.ok(
                planningService.suggestBestLocation(LocalDate.parse(date))
        );
    }

    // ✅ Get busy days (analytics)


    // ✅ Detect risky planning
    @GetMapping("/busy-days")
    public ResponseEntity<Map<String, Object>> getBusyDays(
            @RequestParam Long locationId) {

        return ResponseEntity.ok(
                planningService.getBusyDays(locationId)
        );
    }

    // ✅ Detect risky planning - CORRIGÉ
    @GetMapping("/high-risk")
    public ResponseEntity<Map<String, Object>> isHighRisk(
            @RequestParam Long sessionId) {

        return ResponseEntity.ok(
                planningService.isHighRiskPlanning(sessionId)
        );
    }
    // ✅ Smart date suggestion (skip weekends + conflicts)
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