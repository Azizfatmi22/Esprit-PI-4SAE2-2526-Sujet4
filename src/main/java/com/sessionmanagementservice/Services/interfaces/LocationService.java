package com.sessionmanagementservice.Services.interfaces;

import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;

import java.util.List;

public interface LocationService {

    Location createLocation(Location location);

    Location updateLocation(Long id, Location location);

    void deleteLocation(Long id);

    Location getLocationById(Long id);

    List<Location> getAllLocations();

    List<Location> getLocationsByType(LocationType type);

    public boolean isValidLocation(Location location);
    public List<Location> getOnlineLocations();
    public List<Location> findAvailableLocations(int requiredCapacity);
    public Location findLeastUsedLocation();
    public List<Location> findOverloadedLocations(int threshold);
    public List<Location> searchLocations(String keyword);
    public Location suggestBestLocation(int requiredCapacity, LocationType type);
}
