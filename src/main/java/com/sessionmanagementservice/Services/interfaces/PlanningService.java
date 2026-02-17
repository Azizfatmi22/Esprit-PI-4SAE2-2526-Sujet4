package com.sessionmanagementservice.Services.interfaces;

import com.sessionmanagementservice.entities.Planning;

import java.util.List;

public interface PlanningService {

    Planning createPlanning(Planning planning, int sessionId, Long locationId);

    Planning updatePlanning(int id, Planning planning);

    void deletePlanning(int id);

    Planning getPlanningById(int id);

    List<Planning> getPlanningsBySession(Long sessionId);


}
