package com.sessionmanagementservice.Repos;

import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void shouldFindByType() {
        Location loc = new Location();
        loc.setName("Room A");
        loc.setType(LocationType.ROOM);
        loc.setCapacity(20);

        locationRepository.save(loc);

        assertFalse(locationRepository.findByType(LocationType.ROOM).isEmpty());
    }

    @Test
    void shouldCheckExistsByName() {
        Location loc = new Location();
        loc.setName("Room B");
        loc.setType(LocationType.ROOM);
        loc.setCapacity(10);

        locationRepository.save(loc);

        assertTrue(locationRepository.existsByName("Room B"));
    }

    @Test
    void shouldFindByPlatformUrl() {
        Location loc = new Location();
        loc.setName("Online");
        loc.setType(LocationType.ONLINE_PLATFORM);
        loc.setPlatformUrl("zoom.com");

        locationRepository.save(loc);

        Optional<Location> result = locationRepository.findByPlatformUrl("zoom.com");

        assertTrue(result.isPresent());
    }
}