package com.sessionmanagementservice.Services.impl;

import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.Services.interfaces.PlanningService;
import com.sessionmanagementservice.Services.interfaces.SessionService;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;
import com.sessionmanagementservice.entities.Planning;
import com.sessionmanagementservice.entities.Session;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PlanningServiceImpl implements PlanningService {

    private final PlanningRepository planningRepository;
    private final SessionRepository sessionRepository;
    private final LocationRepository locationRepository;
    private final SessionService sessionService;
    public PlanningServiceImpl(PlanningRepository planningRepository,
                               SessionRepository sessionRepository,
                               LocationRepository locationRepository,
                               SessionService sessionService) {
        this.planningRepository = planningRepository;
        this.sessionRepository = sessionRepository;
        this.locationRepository = locationRepository;
        this.sessionService = sessionService;
    }

    @Override
    public Planning createPlanning(Planning planning, Long sessionId, Long locationId) {

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
            Session session = sessionRepository.findById(planning.getSession().getId())
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
    @Override
    public List<Planning> generatePlanning(Long sessionId, Long locationId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        List<Planning> plannings = new ArrayList<>();

        // Récupérer le nombre de participants de la session
        int participants = session.getMaxParticipants();

        // Calculer le nombre de jours nécessaires (valeur par défaut)
        int totalDays = 5; // Valeur par défaut

        LocalDate start = LocalDate.now(); // Ou une date par défaut
        int daysAdded = 0;

        // Heures totales à répartir (valeur par défaut)
        int totalHoursToDistribute = 35; // 5 jours * 7h

        for (int i = 0; i < totalDays; i++) {
            LocalDate date = start.plusDays(daysAdded);

            // Éviter les weekends
            while (!isValidDay(date)) {
                date = date.plusDays(1);
                daysAdded++;
            }

            // Éviter les conflits
            while (hasPlanningConflict(locationId, date, date)) {
                date = date.plusDays(1);
                daysAdded++;
            }

            Planning p = new Planning();
            p.setSession(session);
            p.setLocation(location);
            p.setStartDate(date);
            p.setEndDate(date);
            p.setTotalHours(7); // 7 heures par jour par défaut

            plannings.add(p);
            daysAdded++;
        }

        return planningRepository.saveAll(plannings);
    }

    private int calculateMaxSessionsPerDay(Location location, int participants) {
        if (location.getType() == LocationType.ONLINE_PLATFORM) {
            return 10; // Les plateformes en ligne peuvent gérer plus de sessions
        }
        // Pour les salles physiques, limiter selon la capacité
        return Math.max(1, location.getCapacity() / participants);
    }
    @Override
    public boolean hasPlanningConflict(Long locationId, LocalDate startDate, LocalDate endDate) {

        List<Planning> existing = planningRepository.findByLocationId(locationId);

        return existing.stream().anyMatch(p ->
                !(p.getEndDate().isBefore(startDate) || p.getStartDate().isAfter(endDate))
        );
    }

    @Override
    public LocalDate suggestNextAvailableDate(Long locationId, LocalDate startDate) {

        LocalDate date = startDate;

        while (hasPlanningConflict(locationId, date, date)) {
            date = date.plusDays(1);
        }

        return date;
    }
    @Override
    public List<Planning> distributePlanning(Long sessionId, Long locationId, int numberOfDays) {

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        List<Planning> result = new java.util.ArrayList<>();

        LocalDate start = session.getCreatedAt();

        for (int i = 0; i < numberOfDays; i++) {

            LocalDate date = suggestNextAvailableDate(locationId, start.plusDays(i));

            Planning p = new Planning();
            p.setSession(session);
            p.setLocation(location);
            p.setStartDate(date);
            p.setEndDate(date);
            p.setTotalHours(2);

            result.add(p);
        }

        return planningRepository.saveAll(result);
    }

    @Override
    public List<Planning> optimizePlanning(Long sessionId) {

        List<Planning> plannings = planningRepository.findBySessionId(sessionId);

        for (Planning p : plannings) {

            if (hasPlanningConflict(
                    p.getLocation().getId(),
                    p.getStartDate(),
                    p.getEndDate())) {

                LocalDate newDate = suggestNextAvailableDate(
                        p.getLocation().getId(),
                        p.getStartDate()
                );

                p.setStartDate(newDate);
                p.setEndDate(newDate);
            }
        }

        return planningRepository.saveAll(plannings);
    }
    @Override
    public long countPlanningsByLocation(Long locationId) {
        return planningRepository.countByLocationId(locationId);
    }
    @Override
    public List<Planning> fillGaps(Long sessionId, Long locationId) {

        List<Planning> plannings = planningRepository.findBySessionId(sessionId)
                .stream()
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .toList();

        List<Planning> result = new java.util.ArrayList<>();

        for (int i = 0; i < plannings.size() - 1; i++) {

            LocalDate currentEnd = plannings.get(i).getEndDate();
            LocalDate nextStart = plannings.get(i + 1).getStartDate();

            if (currentEnd.plusDays(1).isBefore(nextStart)) {

                LocalDate gapDate = currentEnd.plusDays(1);

                if (!hasPlanningConflict(locationId, gapDate, gapDate)) {
                    Planning p = new Planning();
                    p.setSession(plannings.get(i).getSession());
                    p.setLocation(plannings.get(i).getLocation());
                    p.setStartDate(gapDate);
                    p.setEndDate(gapDate);
                    p.setTotalHours(2);

                    result.add(p);
                }
            }
        }

        return planningRepository.saveAll(result);
    }

    public boolean isValidDay(LocalDate date) {
        return !(date.getDayOfWeek().getValue() == 6 || date.getDayOfWeek().getValue() == 7);
    }
    @Override
    public List<Planning> maintainRollingPlanning(Long sessionId, Long locationId, int daysAhead) {

        List<Planning> existing = planningRepository.findBySessionId(sessionId);

        LocalDate lastDate = existing.stream()
                .map(Planning::getEndDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        List<Planning> newPlans = new java.util.ArrayList<>();

        for (int i = 1; i <= daysAhead; i++) {

            LocalDate date = suggestNextAvailableDate(locationId, lastDate.plusDays(i));

            Planning p = new Planning();
            p.setStartDate(date);
            p.setEndDate(date);
            p.setTotalHours(2);

            newPlans.add(p);
        }

        return planningRepository.saveAll(newPlans);
    }
    @Override
    public Location suggestBestLocation(LocalDate date) {

        List<Location> locations = locationRepository.findAll();

        return locations.stream()
                .min((l1, l2) -> Long.compare(
                        countPlanningsByLocation(l1.getId()),
                        countPlanningsByLocation(l2.getId())
                ))
                .orElseThrow(() -> new RuntimeException("No locations available"));
    }
    @Override
    public Map<String, Object>  getBusyDays(Long locationId) {

        List<Planning> plannings = planningRepository.findByLocationId(locationId);

        // Compter les sessions par date en prenant en compte la durée
        Map<LocalDate, Long> sessionsByDate = new HashMap<>();

        for (Planning p : plannings) {
            LocalDate current = p.getStartDate();
            while (!current.isAfter(p.getEndDate())) {
                sessionsByDate.merge(current, 1L, Long::sum);
                current = current.plusDays(1);
            }
        }

        // Formater les résultats pour le frontend
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<LocalDate, Long> entry : sessionsByDate.entrySet()) {
            Map<String, Object> dayInfo = new HashMap<>();
            dayInfo.put("date", entry.getKey().toString());
            dayInfo.put("sessionCount", entry.getValue());

            // Calculer le taux d'occupation (estimation)
            double occupancyRate = Math.min(entry.getValue() * 20.0, 100.0); // 5 sessions max = 100%
            dayInfo.put("occupancyRate", Math.round(occupancyRate));

            // Marquer comme suroccupé si plus de 3 sessions
            dayInfo.put("isOverbooked", entry.getValue() > 3);

            result.add(dayInfo);
        }

        // Trier par date
        result.sort((a, b) -> a.get("date").toString().compareTo(b.get("date").toString()));

        Map<String, Object> response = new HashMap<>();
        response.put("days", result);
        response.put("totalDays", result.size());
        response.put("averageOccupancy", calculateAverageOccupancy(result));

        return response;
    }


    private double calculateAverageOccupancy(List<Map<String, Object>> days) {
        if (days.isEmpty()) return 0;
        double sum = days.stream()
                .mapToDouble(d -> ((Number) d.get("occupancyRate")).doubleValue())
                .sum();
        return Math.round((sum / days.size()) * 10) / 10.0;
    }

    @Override
    public Map<String, Object> isHighRiskPlanning(Long sessionId)  {

        List<Planning> plannings = planningRepository.findBySessionId(sessionId);

        Map<String, Object> result = new HashMap<>();
        List<String> riskFactors = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int riskScore = 0;

        if (plannings.isEmpty()) {
            result.put("isHighRisk", false);
            result.put("riskScore", 0);
            result.put("riskFactors", riskFactors);
            result.put("recommendations", recommendations);
            return result;
        }

        Planning mainPlanning = plannings.get(0);

        // 1. Analyser la charge horaire
        long daysBetween = ChronoUnit.DAYS.between(mainPlanning.getStartDate(), mainPlanning.getEndDate()) + 1;
        int totalHours = mainPlanning.getTotalHours();
        double hoursPerDay = (double) totalHours / daysBetween;

        if (hoursPerDay > 8) {
            riskScore += 30;
            riskFactors.add(String.format("Charge trop élevée: %.1fh/jour", hoursPerDay));
            recommendations.add(String.format("Répartir sur %d jours", (int)Math.ceil(totalHours / 8.0)));
        } else if (hoursPerDay < 4 && daysBetween > 1) {
            riskScore += 10;
            riskFactors.add(String.format("Charge trop faible: %.1fh/jour", hoursPerDay));
            recommendations.add("Regrouper sur moins de jours");
        }

        // 2. Sessions rapprochées (votre logique)
        long closeSessions = plannings.stream()
                .filter(p -> p.getStartDate().plusDays(1).equals(p.getEndDate()))
                .count();

        if (closeSessions > 3) {
            riskScore += 25;
            riskFactors.add("Trop de sessions consécutives");
            recommendations.add("Espacer les sessions");
        }

        // 3. Proximité avec la date actuelle
        LocalDate now = LocalDate.now();
        if (mainPlanning.getStartDate().isBefore(now.plusDays(7))) {
            riskScore += 15;
            riskFactors.add("Planning trop proche");
            recommendations.add("Prévoir plus de délai");
        }

        // 4. Vérifier les weekends
        boolean hasWeekend = false;
        LocalDate current = mainPlanning.getStartDate();
        while (!current.isAfter(mainPlanning.getEndDate())) {
            if (current.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                hasWeekend = true;
                break;
            }
            current = current.plusDays(1);
        }

        if (hasWeekend) {
            riskScore += 10;
            riskFactors.add("Inclut des weekends");
        }

        // 5. Conflits de lieu
        if (mainPlanning.getLocation() != null) {
            List<Planning> conflicts = planningRepository
                    .findByLocationIdAndDateRange(
                            mainPlanning.getLocation().getId(),
                            mainPlanning.getStartDate(),
                            mainPlanning.getEndDate()
                    );

            if (conflicts.size() > 1) { // Plus que le planning actuel
                riskScore += 20;
                riskFactors.add("Conflit avec d'autres plannings");
                recommendations.add("Changer de date ou de lieu");
            }
        }

        result.put("isHighRisk", riskScore > 50);
        result.put("riskScore", riskScore);
        result.put("riskFactors", riskFactors);
        result.put("recommendations", recommendations);
        result.put("totalSessions", plannings.size());
        result.put("hoursPerDay", Math.round(hoursPerDay * 10) / 10.0);

        return result;
    }
    @Override
    public LocalDate smartSuggestDate(Long locationId, LocalDate start) {

        LocalDate date = start;

        while (true) {

            boolean conflict = hasPlanningConflict(locationId, date, date);
            boolean weekend = !isValidDay(date);

            if (!conflict && !weekend) {
                return date;
            }

            date = date.plusDays(1);
        }
    }

    


}
