package com.sessionmanagementservice.Repos;

import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PlanningRepositoryTest {

    @Autowired
    private PlanningRepository planningRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private Location testLocation;
    private Session testSession;

    @BeforeEach
    void setUp() {
        testLocation = new Location();
        testLocation.setName("Room B");
        testLocation.setType(LocationType.ROOM);
        testLocation.setCapacity(10);
        testLocation = locationRepository.save(testLocation);

        testSession = new Session();
        testSession.setStatus(SessionStatus.PLANNED);
        testSession.setMaxParticipants(20);
        testSession.setCreatedAt(LocalDate.now());
        testSession = sessionRepository.save(testSession);
    }

    @Test
    void shouldFindByLocationId() {
        Planning p = new Planning();
        p.setLocation(testLocation);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(1));
        planningRepository.save(p);

        List<Planning> result = planningRepository.findByLocationId(testLocation.getId());

        assertFalse(result.isEmpty());
    }

    @Test
    void shouldFindConflictingPlannings() {
        Planning p = new Planning();
        p.setLocation(testLocation);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(3));
        planningRepository.save(p);

        List<Planning> conflicts = planningRepository.findConflictingPlannings(
                testLocation.getId(),
                LocalDate.now(),
                LocalDate.now()
        );

        assertFalse(conflicts.isEmpty());
    }

    @Test
    void shouldFindBySessionId() {
        Planning p = new Planning();
        p.setLocation(testLocation);
        p.setSession(testSession);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(1));
        planningRepository.save(p);

        List<Planning> result = planningRepository.findBySessionId(testSession.getId());

        assertFalse(result.isEmpty());
    }

    @Test
    void shouldFindFirstBySessionId() {
        Planning p = new Planning();
        p.setLocation(testLocation);
        p.setSession(testSession);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(1));
        planningRepository.save(p);

        Optional<Planning> result = planningRepository.findFirstBySessionId(testSession.getId());

        assertTrue(result.isPresent());
    }
}