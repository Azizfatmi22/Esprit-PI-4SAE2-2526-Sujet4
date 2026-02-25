package com.sessionmanagementservice.Services.impl;



import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.Services.interfaces.LocationService;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {


    private final LocationRepository locationRepository;
    private final PlanningRepository planningRepository;
    public LocationServiceImpl(LocationRepository locationRepository,PlanningRepository planningRepository) {
        this.locationRepository = locationRepository;
        this.planningRepository = planningRepository;
    }

    @Override
    public Location createLocation(Location location) {

        if (locationRepository.existsByName(location.getName())) {
            throw new RuntimeException("Location with this name already exists");
        }

        return locationRepository.save(location);
    }

    @Override
    public Location updateLocation(Long id, Location location) {

        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        existing.setName(location.getName());
        existing.setType(location.getType());
        existing.setCapacity(location.getCapacity());
        existing.setAddress(location.getAddress());
        existing.setPlatformUrl(location.getPlatformUrl());

        return locationRepository.save(existing);
    }

    @Override
    public void deleteLocation(Long id) {

        if (!locationRepository.existsById(id)) {
            throw new RuntimeException("Location not found");
        }

        locationRepository.deleteById(id);
    }

    @Override
    public Location getLocationById(Long id) {

        return locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found"));
    }

    @Override
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    @Override
    public List<Location> getLocationsByType(LocationType type) {
        return locationRepository.findByType(type);
    }

    @Override
    public List<Location> findAvailableLocations(int requiredCapacity) {

        return locationRepository.findAll().stream()
                .filter(loc -> loc.getCapacity() >= requiredCapacity)
                .toList();
    }

    @Override
    public Location findLeastUsedLocation() {

        return locationRepository.findAll().stream()
                .min((l1, l2) -> Long.compare(
                        planningRepository.countByLocationId(l1.getId()),
                        planningRepository.countByLocationId(l2.getId())
                ))
                .orElseThrow(() -> new RuntimeException("No locations found"));
    }

    @Override
    public Location suggestBestLocation(int requiredCapacity, LocationType type) {

        return locationRepository.findByType(type).stream()
                .filter(loc -> loc.getCapacity() >= requiredCapacity)
                .min((l1, l2) -> Integer.compare(l1.getCapacity(), l2.getCapacity()))
                .orElseThrow(() -> new RuntimeException("No suitable location found"));
    }

    @Override
    public List<Location> findOverloadedLocations(int threshold) {

        return locationRepository.findAll().stream()
                .filter(loc ->
                        planningRepository.countByLocationId(loc.getId()) > threshold
                )
                .toList();
    }

    @Override
    public List<Location> searchLocations(String keyword) {

        return locationRepository.findAll().stream()
                .filter(loc ->
                        loc.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                                loc.getAddress().toLowerCase().contains(keyword.toLowerCase())
                )
                .toList();
    }

    @Override
    public List<Location> getOnlineLocations() {

        return locationRepository.findByType(LocationType.ONLINE_PLATFORM);
    }

    @Override
    public boolean isValidLocation(Location location) {

        if (location.getType() == LocationType.ONLINE_PLATFORM) {
            return location.getPlatformUrl() != null && !location.getPlatformUrl().isEmpty();
        }

        return location.getCapacity() > 0 && location.getAddress() != null;
    }
}

