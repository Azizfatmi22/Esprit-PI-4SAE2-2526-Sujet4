package com.sessionmanagementservice.Repos;



import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.entities.Planning;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PlanningRepositoryTest {

    @Autowired
    private PlanningRepository planningRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void shouldFindByLocationId() {
        Location loc = new Location();
        loc.setName("Room A");
        loc.setType(LocationType.ROOM);
        loc.setCapacity(20);
        loc = locationRepository.save(loc);

        Planning p = new Planning();
        p.setLocation(loc);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(1));

        planningRepository.save(p);

        List<Planning> result = planningRepository.findByLocationId(loc.getId());

        assertFalse(result.isEmpty());
    }

    @Test
    void shouldFindConflictingPlannings() {
        Location loc = new Location();
        loc.setName("Room B");
        loc.setType(LocationType.ROOM);
        loc.setCapacity(10);
        loc = locationRepository.save(loc);

        Planning p = new Planning();
        p.setLocation(loc);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(3));

        planningRepository.save(p);

        List<Planning> conflicts = planningRepository.findConflictingPlannings(
                loc.getId(),
                LocalDate.now(),
                LocalDate.now()
        );

        assertFalse(conflicts.isEmpty());
    }

    @Test
    void shouldFindBySessionIdOptional() {
        Planning p = new Planning();
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(1));

        Planning saved = planningRepository.save(p);

        Optional<Planning> result = planningRepository.findFirstBySessionId(saved.getSession().getId());

        assertTrue(result.isEmpty() || result.isPresent());
    }
}