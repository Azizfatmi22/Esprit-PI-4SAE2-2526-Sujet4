package com.sessionmanagementservice.Repos;

import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    private Location roomLocation;
    private Location onlineLocation;

    @BeforeEach
    void setUp() {
        // Create test data
        roomLocation = new Location();
        roomLocation.setName("Room A");
        roomLocation.setType(LocationType.ROOM);
        roomLocation.setCapacity(20);
        roomLocation.setAddress("Test Address");

        onlineLocation = new Location();
        onlineLocation.setName("Online");
        onlineLocation.setType(LocationType.ONLINE_PLATFORM);
        onlineLocation.setPlatformUrl("zoom.com");
        onlineLocation.setCapacity(100);
    }

    @Test
    void shouldFindByType() {
        // Save test data
        locationRepository.save(roomLocation);

        // Test find by type
        var result = locationRepository.findByType(LocationType.ROOM);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Room A", result.get(0).getName());
    }

    @Test
    void shouldCheckExistsByName() {
        // Save test data
        locationRepository.save(roomLocation);

        // Test exists by name
        assertTrue(locationRepository.existsByName("Room A"));
        assertFalse(locationRepository.existsByName("Non Existent Room"));
    }

    @Test
    void shouldFindByPlatformUrl() {
        // Save test data
        locationRepository.save(onlineLocation);

        // Test find by platform URL
        Optional<Location> result = locationRepository.findByPlatformUrl("zoom.com");

        assertTrue(result.isPresent());
        assertEquals("Online", result.get().getName());
        assertEquals(LocationType.ONLINE_PLATFORM, result.get().getType());
    }

    @Test
    void shouldFindByPlatformUrlNotFound() {
        // Test when platform URL doesn't exist
        Optional<Location> result = locationRepository.findByPlatformUrl("nonexistent.com");

        assertFalse(result.isPresent());
    }

    @Test
    void shouldSaveAndRetrieveLocation() {
        // Save location
        Location saved = locationRepository.save(roomLocation);

        // Retrieve and verify
        Optional<Location> retrieved = locationRepository.findById(saved.getId());

        assertTrue(retrieved.isPresent());
        assertEquals("Room A", retrieved.get().getName());
        assertEquals(LocationType.ROOM, retrieved.get().getType());
        assertEquals(20, retrieved.get().getCapacity());
    }

    @Test
    void shouldDeleteLocation() {
        // Save location
        Location saved = locationRepository.save(roomLocation);

        // Delete it
        locationRepository.deleteById(saved.getId());

        // Verify it's gone
        assertFalse(locationRepository.existsById(saved.getId()));
    }
}