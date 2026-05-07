package com.sessionmanagementservice.controllers;

import com.sessionmanagementservice.Services.interfaces.LocationService;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    public ResponseEntity<?> createLocation(@RequestBody Location location) {
        try {
            // Validate required fields
            if (location.getName() == null || location.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Name is required");
            }
            if (location.getType() == null) {
                return ResponseEntity.badRequest().body("Type is required");
            }
            if (location.getCapacity() == null || location.getCapacity() <= 0) {
                return ResponseEntity.badRequest().body("Valid capacity is required");
            }

            Location created = locationService.createLocation(location);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLocationById(@PathVariable Long id) {
        try {
            Location location = locationService.getLocationById(id);
            return ResponseEntity.ok(location);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Location>> getAllLocations() {
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLocation(@PathVariable Long id, @RequestBody Location location) {
        try {
            Location updated = locationService.updateLocation(id, location);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable Long id) {
        try {
            locationService.deleteLocation(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateLocation(@RequestBody Location location) {
        return ResponseEntity.ok(locationService.isValidLocation(location));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Location>> searchLocations(@RequestParam String keyword) {
        return ResponseEntity.ok(locationService.searchLocations(keyword));
    }

    @GetMapping("/suggest")
    public ResponseEntity<Location> suggestLocation(
            @RequestParam int capacity,
            @RequestParam LocationType type) {
        Location suggested = locationService.suggestBestLocation(capacity, type);
        return ResponseEntity.ok(suggested);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Location>> getAvailableLocations(@RequestParam int capacity) {
        return ResponseEntity.ok(locationService.findAvailableLocations(capacity));
    }
}