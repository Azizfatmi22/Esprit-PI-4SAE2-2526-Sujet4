package com.sessionmanagementservice.Services.impl;

import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.Services.interfaces.PlanningService;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;
import com.sessionmanagementservice.entities.Planning;
import com.sessionmanagementservice.entities.Session;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class PlanningServiceImpl implements PlanningService {

    private final PlanningRepository planningRepository;
    private final SessionRepository sessionRepository;
    private final LocationRepository locationRepository;

    public PlanningServiceImpl(PlanningRepository planningRepository,
                               SessionRepository sessionRepository,
                               LocationRepository locationRepository) {
        this.planningRepository = planningRepository;
        this.sessionRepository = sessionRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public Planning createPlanning(Planning planning, int sessionId, Long locationId) {

        if (planning.getStartDate().isAfter(planning.getEndDate())) {
            throw new RuntimeException("Invalid date range");
        }

        // Fetch session
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Location location = null;

        // Online session via platformUrl
        if (planning.getLocation() != null && planning.getLocation().getPlatformUrl() != null) {
            String platformUrl = planning.getLocation().getPlatformUrl();
            location = locationRepository.findByPlatformUrl(platformUrl)
                    .orElseGet(() -> {
                        Location newLoc = new Location();
                        newLoc.setName("Online Platform");
                        newLoc.setType(LocationType.ONLINE_PLATFORM);
                        newLoc.setCapacity(0);
                        newLoc.setAddress("Online");
                        newLoc.setPlatformUrl(platformUrl);
                        return locationRepository.save(newLoc);
                    });
        }
        // Offline session via locationId
        else if (locationId != null) {
            location = locationRepository.findById(locationId)
                    .orElseThrow(() -> new RuntimeException("Location not found"));
        }
        // If still null, use default "Unassigned" location
        else {
            location = new Location();
            location.setName("Unassigned Location");
            location.setType(LocationType.ONLINE_PLATFORM);
            location.setCapacity(0);
            location.setAddress("Unknown");
            location = locationRepository.save(location);
        }

        // Attach session and location
        planning.setSession(session);
        planning.setLocation(location);

        return planningRepository.save(planning);
    }

    @Override
    public Planning updatePlanning(int id, Planning planning) {
        Planning existing = planningRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Planning not found"));

        if (planning.getStartDate().isAfter(planning.getEndDate())) {
            throw new RuntimeException("Invalid date range");
        }

        // Update session if provided
        if (planning.getSession() != null && planning.getSession().getId() != null) {
            Session session = sessionRepository.findById(planning.getSession().getId().intValue())
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            existing.setSession(session);
        }

        // Update location
        if (planning.getLocation() != null) {
            if (planning.getLocation().getId() != null) {
                // Offline location
                Location location = locationRepository.findById(planning.getLocation().getId())
                        .orElseThrow(() -> new RuntimeException("Location not found"));
                existing.setLocation(location);
            } else if (planning.getLocation().getPlatformUrl() != null) {
                // Online location
                String platformUrl = planning.getLocation().getPlatformUrl();
                Location location = locationRepository.findByPlatformUrl(platformUrl)
                        .orElseGet(() -> {
                            Location newLoc = new Location();
                            newLoc.setName("Online Platform");
                            newLoc.setType(LocationType.ONLINE_PLATFORM);
                            newLoc.setCapacity(0);
                            newLoc.setAddress("Online");
                            newLoc.setPlatformUrl(platformUrl);
                            return locationRepository.save(newLoc);
                        });
                existing.setLocation(location);
            }
        }

        // Update simple fields
        existing.setMode(planning.getMode());
        existing.setTotalHours(planning.getTotalHours());
        existing.setStartDate(planning.getStartDate());
        existing.setEndDate(planning.getEndDate());

        return planningRepository.save(existing);
    }

    @Override
    public void deletePlanning(int id) {
        planningRepository.deleteById(id);
    }

    @Override
    public Planning getPlanningById(int id) {
        return planningRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Planning not found"));
    }

    @Override
    public List<Planning> getPlanningsBySession(Long sessionId) {
        return planningRepository.findBySessionId(sessionId);
    }
}
