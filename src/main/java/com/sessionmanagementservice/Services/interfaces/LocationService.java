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
}
