package com.sessionmanagementservice.Services.impl;

import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.Services.interfaces.PlanningService;
import com.sessionmanagementservice.Services.interfaces.SessionService;
import com.sessionmanagementservice.entities.*;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlanningServiceImpl implements PlanningService {

    private final PlanningRepository planningRepository;
    private final SessionRepository sessionRepository;
    private final LocationRepository locationRepository;
    private final SessionService sessionService;

    // Constantes
    private static final int MAX_HOURS_PER_DAY = 7;
    private static final int DEFAULT_TOTAL_HOURS = 35;
    private static final int MAX_ATTEMPTS = 60;

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
        validateDateRange(planning.getStartDate(), planning.getEndDate());

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));


        if (planning.getStartDate().isBefore(session.getCreatedAt())) {
            throw new RuntimeException("Start date cannot be before session creation date");
        }
        Location location = determineLocation(planning, locationId);

        planning.setSession(session);
        planning.setLocation(location);

        return planningRepository.save(planning);
    }

    private Location determineLocation(Planning planning, Long locationId) {
        // Online session via platformUrl
        if (planning.getLocation() != null && planning.getLocation().getPlatformUrl() != null) {
            String platformUrl = planning.getLocation().getPlatformUrl();
            return locationRepository.findByPlatformUrl(platformUrl)
                    .orElseGet(() -> createOnlineLocation(platformUrl));
        }
        // Offline session via locationId
        else if (locationId != null) {
            return locationRepository.findById(locationId)
                    .orElseThrow(() -> new RuntimeException("Location not found"));
        }
        // Default unassigned location
        else {
            return createUnassignedLocation();
        }
    }

    private Location createOnlineLocation(String platformUrl) {
        Location newLoc = new Location();
        newLoc.setName("Online Platform");
        newLoc.setType(LocationType.ONLINE_PLATFORM);
        newLoc.setCapacity(0);
        newLoc.setAddress("Online");
        newLoc.setPlatformUrl(platformUrl);
        return locationRepository.save(newLoc);
    }

    private Location createUnassignedLocation() {
        Location newLoc = new Location();
        newLoc.setName("Unassigned Location");
        newLoc.setType(LocationType.ONLINE_PLATFORM);
        newLoc.setCapacity(0);
        newLoc.setAddress("Unknown");
        return locationRepository.save(newLoc);
    }

    @Override
    public Planning updatePlanning(int id, Planning planning) {
        Planning existing = planningRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Planning not found"));

        validateDateRange(planning.getStartDate(), planning.getEndDate());

        // Update session if provided
        if (planning.getSession() != null && planning.getSession().getId() != null) {
            Session session = sessionRepository.findById(planning.getSession().getId())
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            existing.setSession(session);
        }

        // Update location
        if (planning.getLocation() != null) {
            updatePlanningLocation(existing, planning);
        }

        // Update simple fields
        existing.setMode(planning.getMode());
        existing.setTotalHours(planning.getTotalHours());
        existing.setStartDate(planning.getStartDate());
        existing.setEndDate(planning.getEndDate());

        return planningRepository.save(existing);
    }

    private void updatePlanningLocation(Planning existing, Planning planning) {
        if (planning.getLocation().getId() != null) {
            Location location = locationRepository.findById(planning.getLocation().getId())
                    .orElseThrow(() -> new RuntimeException("Location not found"));
            existing.setLocation(location);
        } else if (planning.getLocation().getPlatformUrl() != null) {
            String platformUrl = planning.getLocation().getPlatformUrl();
            Location location = locationRepository.findByPlatformUrl(platformUrl)
                    .orElseGet(() -> createOnlineLocation(platformUrl));
            existing.setLocation(location);
        }
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

    // ==================== MÉTHODES CORRIGÉES (UN SEUL PLANNING) ====================

    @Override
    public Planning generatePlanning(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // 🔍 Meilleur lieu
        Location bestLocation = findBestLocationForSession(session);

        // Supprimer l'ancien planning
        planningRepository.deleteBySessionId(sessionId);
        planningRepository.flush();

        // ⏱️ Calculer la durée
        int totalHours = DEFAULT_TOTAL_HOURS;
        int daysNeeded = (int) Math.ceil((double) totalHours / MAX_HOURS_PER_DAY);

        // 📅 Trouver la date de début
        LocalDate startDate = findNextAvailableStartDate(bestLocation.getId(), daysNeeded);
        long duration = daysNeeded - 1L;
        LocalDate endDate = startDate.plusDays(duration);

        // 📝 Créer le planning
        Planning planning = new Planning();
        planning.setSession(session);
        planning.setLocation(bestLocation);
        planning.setStartDate(startDate);
        planning.setEndDate(endDate);
        planning.setTotalHours(totalHours);
        planning.setMode(PlanningMode.ONSITE);

        return planning;
    }

    private LocalDate findNextAvailableStartDate(Long locationId, int daysNeeded) {
        LocalDate date = LocalDate.now().plusDays(1);
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            boolean available = true;
            for (int i = 0; i < daysNeeded; i++) {
                LocalDate checkDate = date.plusDays(i);
                if (!isValidDay(checkDate) || hasPlanningConflict(locationId, checkDate, checkDate)) {
                    available = false;
                    break;
                }
            }
            if (available) return date;
            date = date.plusDays(1);
            attempts++;
        }

        throw new RuntimeException("Impossible de trouver " + daysNeeded +
                " jours consécutifs disponibles dans les " + MAX_ATTEMPTS + " prochains jours");
    }

    @Override
    public Planning distributePlanning(Long sessionId, Long locationId, int numberOfDays) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session non trouvée"));
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé"));

        // Supprimer l'ancien planning
        planningRepository.deleteBySessionId(sessionId);
        planningRepository.flush();

        int totalHours = DEFAULT_TOTAL_HOURS;

        // 📅 Trouver la date de début
        LocalDate startDate = findNextAvailableStartDate(locationId, numberOfDays);
        LocalDate endDate = startDate.plusDays((long) numberOfDays - 1);

        // 📝 Créer un seul planning
        Planning planning = new Planning();
        planning.setSession(session);
        planning.setLocation(location);
        planning.setStartDate(startDate);
        planning.setEndDate(endDate);
        planning.setTotalHours(totalHours);
        planning.setMode(PlanningMode.ONSITE);

        return planningRepository.save(planning);
    }

    @Override
    public Planning optimizePlanning(Long sessionId) {
        Planning planning = planningRepository.findFirstBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Planning non trouvé pour cette session"));

        boolean hasConflict = hasPlanningConflict(
                planning.getLocation().getId(),
                planning.getStartDate(),
                planning.getEndDate()
        );

        if (hasConflict) {
            // Calculer la durée en jours
            long daysBetween = ChronoUnit.DAYS.between(planning.getStartDate(), planning.getEndDate()) + 1;
            int daysNeeded = (int) daysBetween;

            // Trouver une nouvelle plage de dates
            LocalDate newStartDate = findNextAvailableStartDate(planning.getLocation().getId(), daysNeeded);
            LocalDate newEndDate = newStartDate.plusDays((long) daysNeeded - 1);

            planning.setStartDate(newStartDate);
            planning.setEndDate(newEndDate);

            System.out.println("Planning optimisé: nouvelle période du " + newStartDate + " au " + newEndDate);
        }

        return planningRepository.save(planning);
    }

    @Override
    public Planning fillGaps(Long sessionId, Long locationId) {
        // Cette méthode n'a pas de sens avec un seul planning
        // On retourne le planning existant ou on lève une exception
        return planningRepository.findFirstBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Aucun planning trouvé"));
    }

    @Override
    public Planning maintainRollingPlanning(Long sessionId, Long locationId, int daysAhead) {
        Planning existing = planningRepository.findFirstBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Planning non trouvé"));

        // Étendre la date de fin
        LocalDate newEndDate = existing.getEndDate().plusDays(daysAhead);

        // Vérifier que les nouvelles dates sont disponibles
        LocalDate currentDate = existing.getEndDate().plusDays(1);
        while (!currentDate.isAfter(newEndDate)) {
            if (!isValidDay(currentDate) || hasPlanningConflict(locationId, currentDate, currentDate)) {
                throw new RuntimeException("La date " + currentDate + " n'est pas disponible");
            }
            currentDate = currentDate.plusDays(1);
        }

        existing.setEndDate(newEndDate);

        // Ajuster les heures totales (optionnel)
        int additionalHours = daysAhead * MAX_HOURS_PER_DAY;
        existing.setTotalHours(existing.getTotalHours() + additionalHours);

        return planningRepository.save(existing);
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private LocalDate findNextAvailableDate(Long locationId, LocalDate startDate) {
        LocalDate date = startDate;
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            if (isValidDay(date) && !hasPlanningConflict(locationId, date, date)) {
                return date;
            }
            date = date.plusDays(1);
            attempts++;
        }

        throw new RuntimeException("Aucune date disponible trouvée dans les " + MAX_ATTEMPTS + " prochains jours");
    }

    @Override
    public boolean hasPlanningConflict(Long locationId, LocalDate startDate, LocalDate endDate) {
        if (locationId == null || startDate == null || endDate == null) {
            return false;
        }

        List<Planning> conflicts = planningRepository.findConflictingPlannings(
                locationId, startDate, endDate
        );

        return !conflicts.isEmpty();
    }

    @Override
    public LocalDate suggestNextAvailableDate(Long locationId, LocalDate startDate) {
        LocalDate date = startDate;
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            if (isValidDay(date) && !hasPlanningConflict(locationId, date, date)) {
                return date;
            }
            date = date.plusDays(1);
            attempts++;
        }

        throw new RuntimeException("Aucune date disponible trouvée dans les " + MAX_ATTEMPTS + " prochains jours");
    }

    @Override
    public boolean isValidDay(LocalDate date) {
        if (date == null) return false;
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    @Override
    public long countPlanningsByLocation(Long locationId) {
        return planningRepository.countByLocationId(locationId);
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
    public Map<String, Object> getBusyDays(Long locationId) {
        List<Planning> plannings = planningRepository.findByLocationId(locationId);

        Map<LocalDate, Integer> sessionsByDate = new HashMap<>();
        Map<LocalDate, Integer> hoursByDate = new HashMap<>();

        for (Planning p : plannings) {
            LocalDate current = p.getStartDate();
            while (!current.isAfter(p.getEndDate())) {
                sessionsByDate.merge(current, 1, Integer::sum);
                hoursByDate.merge(current, p.getTotalHours(), Integer::sum);
                current = current.plusDays(1);
            }
        }

        List<Map<String, Object>> days = new ArrayList<>();
        int maxSessionsPerDay = 5;

        for (Map.Entry<LocalDate, Integer> entry : sessionsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            int sessionCount = entry.getValue();
            int totalHours = hoursByDate.getOrDefault(date, 0);

            Map<String, Object> dayInfo = new HashMap<>();
            dayInfo.put("date", date.toString());
            dayInfo.put("sessionCount", sessionCount);
            dayInfo.put("totalHours", totalHours);

            double occupancyRate = Math.min((sessionCount * 100.0) / maxSessionsPerDay, 100.0);
            dayInfo.put("occupancyRate", Math.round(occupancyRate));

            String busyLevel;
            if (sessionCount <= 2) busyLevel = "FAIBLE";
            else if (sessionCount <= 4) busyLevel = "MOYEN";
            else busyLevel = "ELEVE";
            dayInfo.put("busyLevel", busyLevel);

            dayInfo.put("isOverbooked", sessionCount > maxSessionsPerDay);

            days.add(dayInfo);
        }

        days.sort((a, b) -> a.get("date").toString().compareTo(b.get("date").toString()));

        Map<String, Object> response = new HashMap<>();
        response.put("days", days);
        response.put("totalDays", days.size());
        response.put("averageOccupancy", calculateAverageOccupancy(days));

        if (!days.isEmpty()) {
            long overbookedCount = days.stream().filter(d -> (boolean)d.get("isOverbooked")).count();
            response.put("overbookedDays", overbookedCount);
        }

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
    public Map<String, Object> isHighRiskPlanning(Long sessionId) {
        Planning planning = planningRepository.findFirstBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Planning non trouvé"));

        Map<String, Object> result = new HashMap<>();
        List<String> riskFactors = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int riskScore = 0;

        Session session = planning.getSession();

        // Analyse de la charge horaire
        riskScore += analyzeHourlyLoad(planning, riskFactors, recommendations);

        // Analyse de la capacité
        riskScore += analyzeCapacity(planning, session, riskFactors, recommendations);

        // Analyse de la proximité
        riskScore += analyzeProximity(planning, riskFactors, recommendations);

        // Analyse des weekends
        riskScore += analyzeWeekends(planning, riskFactors, recommendations);

        // Analyse des conflits
        riskScore += analyzeConflicts(planning, riskFactors, recommendations);

        result.put("isHighRisk", riskScore > 50);
        result.put("riskScore", riskScore);
        result.put("riskFactors", riskFactors);
        result.put("recommendations", recommendations);

        return result;
    }

    private int analyzeHourlyLoad(Planning planning, List<String> riskFactors, List<String> recommendations) {
        long daysBetween = ChronoUnit.DAYS.between(planning.getStartDate(), planning.getEndDate()) + 1;
        double hoursPerDay = (double) planning.getTotalHours() / daysBetween;

        if (hoursPerDay > 8) {
            riskFactors.add(String.format("Charge trop élevée: %.1fh/jour", hoursPerDay));
            recommendations.add(String.format("Étaler sur %d jours", (int)Math.ceil(planning.getTotalHours() / 8.0)));
            return 30;
        } else if (hoursPerDay < 4 && daysBetween > 1) {
            riskFactors.add(String.format("Charge faible: %.1fh/jour", hoursPerDay));
            recommendations.add("Regrouper sur moins de jours");
            return 10;
        }
        return 0;
    }

    private int analyzeCapacity(Planning planning, Session session,
                                List<String> riskFactors, List<String> recommendations) {
        if (planning.getLocation() == null) return 0;

        int capacity = planning.getLocation().getCapacity();
        int participants = session.getMaxParticipants();

        if (participants > capacity) {
            riskFactors.add(String.format("Capacité insuffisante: %d > %d", participants, capacity));
            recommendations.add("Changer pour un lieu plus grand");
            return 40;
        } else if (participants > capacity * 0.9) {
            riskFactors.add(String.format("Capacité limite: %d/%d", participants, capacity));
            recommendations.add("Prévoir un plan B");
            return 20;
        }
        return 0;
    }

    private int analyzeProximity(Planning planning, List<String> riskFactors, List<String> recommendations) {
        LocalDate now = LocalDate.now();
        if (planning.getStartDate().isBefore(now.plusDays(3))) {
            riskFactors.add("Planning urgent (< 3 jours)");
            recommendations.add("Confirmer rapidement");
            return 20;
        } else if (planning.getStartDate().isBefore(now.plusDays(7))) {
            riskFactors.add("Planning proche (< 1 semaine)");
            return 10;
        }
        return 0;
    }

    private int analyzeWeekends(Planning planning, List<String> riskFactors, List<String> recommendations) {
        long weekendCount = 0;
        LocalDate current = planning.getStartDate();
        while (!current.isAfter(planning.getEndDate())) {
            if (current.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                weekendCount++;
            }
            current = current.plusDays(1);
        }

        if (weekendCount > 0) {
            riskFactors.add(String.format("%d jour(s) de weekend", weekendCount));
            return (int) (weekendCount * 5);
        }
        return 0;
    }

    private int analyzeConflicts(Planning planning, List<String> riskFactors, List<String> recommendations) {
        if (planning.getLocation() == null) return 0;

        List<Planning> conflicts = planningRepository
                .findByLocationIdAndDateRange(
                        planning.getLocation().getId(),
                        planning.getStartDate(),
                        planning.getEndDate()
                );

        int otherConflicts = (int) conflicts.stream()
                .filter(p -> !p.getId().equals(planning.getId()))
                .count();

        if (otherConflicts > 0) {
            riskFactors.add(String.format("Conflit avec %d autre(s) planning(s)", otherConflicts));
            recommendations.add("Vérifier les dates");
            return otherConflicts * 15;
        }
        return 0;
    }

    @Override
    public LocalDate smartSuggestDate(Long locationId, LocalDate start) {
        LocalDate date = start;
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            boolean conflict = hasPlanningConflict(locationId, date, date);
            boolean weekend = !isValidDay(date);

            if (!conflict && !weekend) {
                return date;
            }

            date = date.plusDays(1);
            attempts++;
        }

        throw new RuntimeException("No suitable date found within " + MAX_ATTEMPTS + " days");
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new RuntimeException("Dates cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date cannot be after end date");
        }
    }

    private Location findBestLocationForSession(Session session) {
        List<Location> allLocations = locationRepository.findAll();
        int participants = session.getMaxParticipants();

        Location bestLocation = null;
        int bestScore = -1;

        System.out.println("🔍 Recherche du meilleur lieu pour " + participants + " participants");

        for (Location location : allLocations) {
            if (location.getCapacity() < participants) {
                continue;
            }

            int score = 0;

            if (location.getType() == LocationType.ROOM) {
                score += 50;
            } else if (location.getType() == LocationType.HYBRID) {
                score += 40;
            } else if (location.getType() == LocationType.ONLINE_PLATFORM) {
                score += 30;
            }

            long usageCount = planningRepository.countByLocationId(location.getId());
            score += Math.max(0, 100 - usageCount * 10);

            double capacityRatio = (double) participants / location.getCapacity();
            if (capacityRatio > 0.7 && capacityRatio <= 1.0) {
                score += 30;
            } else if (capacityRatio > 0.5) {
                score += 15;
            } else {
                score -= 10;
            }

            int availabilityScore = calculateAvailabilityScore(location.getId());
            score += availabilityScore;

            if (location.getPlatformUrl() != null && !location.getPlatformUrl().isEmpty()) {
                score += 20;
            }

            System.out.println("  📍 " + location.getName() + " (capacité: " + location.getCapacity() +
                    ", utilisé: " + usageCount + " fois) -> score: " + score);

            if (score > bestScore) {
                bestScore = score;
                bestLocation = location;
            }
        }

        if (bestLocation == null) {
            throw new RuntimeException("Aucun lieu disponible avec capacité suffisante (besoin de " + participants + " places)");
        }

        System.out.println("🏆 MEILLEUR LIEU CHOISI: " + bestLocation.getName() +
                " (score: " + bestScore + ", capacité: " + bestLocation.getCapacity() + ")");

        return bestLocation;
    }

    private int calculateAvailabilityScore(Long locationId) {
        int score = 0;
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 7; i++) {
            LocalDate date = today.plusDays(i);
            if (!hasPlanningConflict(locationId, date, date)) {
                score += 10;
            }
        }

        return score;
    }
}