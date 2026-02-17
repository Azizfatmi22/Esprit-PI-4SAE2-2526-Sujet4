package com.sessionmanagementservice.Services.impl;



import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.Services.interfaces.LocationService;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {


    private final LocationRepository locationRepository;

    public LocationServiceImpl(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
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
}

