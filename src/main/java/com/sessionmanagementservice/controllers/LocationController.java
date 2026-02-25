package com.sessionmanagementservice.controllers;


import com.sessionmanagementservice.Services.interfaces.LocationService;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;

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
    public Location createLocation(@RequestBody Location location) {
        return locationService.createLocation(location);
    }

    @PutMapping("/{id}")
    public Location updateLocation(@PathVariable Long id,
                                   @RequestBody Location location) {
        return locationService.updateLocation(id, location);
    }

    @DeleteMapping("/{id}")
    public void deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
    }

    @GetMapping("/{id}")
    public Location getLocationById(@PathVariable Long id) {
        return locationService.getLocationById(id);
    }

    @GetMapping
    public List<Location> getAllLocations() {
        return locationService.getAllLocations();
    }

    @GetMapping("/type/{type}")
    public List<Location> getLocationsByType(@PathVariable LocationType type) {
        return locationService.getLocationsByType(type);
    }
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validate(@RequestBody Location location) {

        return ResponseEntity.ok(
                locationService.isValidLocation(location)
        );
    }
    @GetMapping("/online")
    public ResponseEntity<List<Location>> online() {

        return ResponseEntity.ok(
                locationService.getOnlineLocations()
        );
    }
    @GetMapping("/search")
    public ResponseEntity<List<Location>> search(
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                locationService.searchLocations(keyword)
        );
    }
    @GetMapping("/overloaded")
    public ResponseEntity<List<Location>> overloaded(
            @RequestParam int threshold) {

        return ResponseEntity.ok(
                locationService.findOverloadedLocations(threshold)
        );
    }
    @GetMapping("/suggest")
    public ResponseEntity<Location> suggestLocation(
            @RequestParam int capacity,
            @RequestParam LocationType type) {

        return ResponseEntity.ok(
                locationService.suggestBestLocation(capacity, type)
        );
    }
    @GetMapping("/least-used")
    public ResponseEntity<Location> getLeastUsedLocation() {

        return ResponseEntity.ok(
                locationService.findLeastUsedLocation()
        );
    }
    @GetMapping("/available")
    public ResponseEntity<List<Location>> getAvailableLocations(
            @RequestParam int capacity) {

        return ResponseEntity.ok(
                locationService.findAvailableLocations(capacity)
        );
    }
}

