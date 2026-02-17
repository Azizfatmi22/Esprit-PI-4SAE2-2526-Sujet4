package com.sessionmanagementservice.Services.impl;

import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.Services.interfaces.PlanningService;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.Planning;
import com.sessionmanagementservice.entities.Session;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

@Transactional
public class PlanningServiceImpl implements PlanningService {

    private final PlanningRepository planningRepository;
    private final SessionRepository sessionRepository;
    private final LocationRepository locationRepository;
    PlanningServiceImpl(PlanningRepository planningRepository,SessionRepository sessionRepository,LocationRepository locationRepository ) {
        this.planningRepository = planningRepository;
        this.sessionRepository = sessionRepository;
        this.locationRepository = locationRepository;

    }

    @Override
    public Planning createPlanning(Planning planning,
                                   int sessionId,
                                   Long locationId) {

        if (planning.getStartDate().isAfter(planning.getEndDate())) {
            throw new RuntimeException("Invalid date range");
        }

        // Fetch session
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Fetch location
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        // Attach them
        planning.setSession(session);
        planning.setLocation(location);

        return planningRepository.save(planning);
    }

    @Override
    public Planning updatePlanning(int id, Planning planning) {

        Planning existing = planningRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Planning not found"));

        // 2️⃣ Validate dates
        if (planning.getStartDate().isAfter(planning.getEndDate())) {
            throw new RuntimeException("Invalid date range");
        }

        // 3️⃣ Fetch and attach managed Session
        if (planning.getSession() != null && planning.getSession().getId() != null) {
            Session session = sessionRepository.findById(planning.getSession().getId().intValue())
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            existing.setSession(session);
        }

        // 4️⃣ Fetch and attach managed Location
        if (planning.getLocation() != null && planning.getLocation().getId() != null) {
            Location location = locationRepository.findById(planning.getLocation().getId())
                    .orElseThrow(() -> new RuntimeException("Location not found"));
            existing.setLocation(location);
        }

        // 5️⃣ Update simple fields
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

