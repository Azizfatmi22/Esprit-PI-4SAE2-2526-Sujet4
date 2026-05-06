package com.sessionmanagementservice.services;



import com.sessionmanagementservice.Repositories.LocationRepository;
import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.Services.impl.LocationServiceImpl;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private PlanningRepository planningRepository;

    @InjectMocks
    private LocationServiceImpl locationService;

    private Location loc1;
    private Location loc2;

    @BeforeEach
    void setup() {
        loc1 = new Location();
        loc1.setId(1L);
        loc1.setName("Room A");
        loc1.setCapacity(50);
        loc1.setAddress("Tunis");
        loc1.setType(LocationType.ROOM);

        loc2 = new Location();
        loc2.setId(2L);
        loc2.setName("Online Zoom");
        loc2.setCapacity(0);
        loc2.setAddress("Online");
        loc2.setPlatformUrl("zoom.com");
        loc2.setType(LocationType.ONLINE_PLATFORM);
    }

    // ✅ CREATE
    @Test
    void shouldCreateLocation() {
        when(locationRepository.existsByName("Room A")).thenReturn(false);
        when(locationRepository.save(any())).thenReturn(loc1);

        Location result = locationService.createLocation(loc1);

        assertNotNull(result);
        assertEquals("Room A", result.getName());
    }

    // ❌ CREATE DUPLICATE
    @Test
    void shouldThrowWhenLocationExists() {
        when(locationRepository.existsByName("Room A")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> locationService.createLocation(loc1));
    }

    // ✅ UPDATE
    @Test
    void shouldUpdateLocation() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(loc1));
        when(locationRepository.save(any())).thenReturn(loc1);

        Location updated = new Location();
        updated.setName("Updated");
        updated.setCapacity(100);
        updated.setAddress("Sousse");
        updated.setType(LocationType.ROOM);

        Location result = locationService.updateLocation(1L, updated);

        assertEquals("Updated", result.getName());
        assertEquals(100, result.getCapacity());
    }

    // ❌ UPDATE NOT FOUND
    @Test
    void shouldThrowWhenUpdateNotFound() {
        when(locationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> locationService.updateLocation(1L, loc1));
    }

    // ✅ DELETE
    @Test
    void shouldDeleteLocation() {
        when(locationRepository.existsById(1L)).thenReturn(true);

        locationService.deleteLocation(1L);

        verify(locationRepository).deleteById(1L);
    }

    // ❌ DELETE NOT FOUND
    @Test
    void shouldThrowWhenDeleteNotFound() {
        when(locationRepository.existsById(1L)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> locationService.deleteLocation(1L));
    }

    // ✅ GET BY ID
    @Test
    void shouldGetLocationById() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(loc1));

        Location result = locationService.getLocationById(1L);

        assertEquals(1L, result.getId());
    }

    // ✅ GET ALL
    @Test
    void shouldGetAllLocations() {
        when(locationRepository.findAll()).thenReturn(List.of(loc1, loc2));

        List<Location> result = locationService.getAllLocations();

        assertEquals(2, result.size());
    }

    // ✅ FIND AVAILABLE
    @Test
    void shouldFindAvailableLocations() {
        when(locationRepository.findAll()).thenReturn(List.of(loc1, loc2));

        List<Location> result = locationService.findAvailableLocations(30);

        assertEquals(1, result.size());
        assertEquals("Room A", result.get(0).getName());
    }

    // ✅ LEAST USED
    @Test
    void shouldFindLeastUsedLocation() {
        when(locationRepository.findAll()).thenReturn(List.of(loc1, loc2));
        when(planningRepository.countByLocationId(1L)).thenReturn(10L);
        when(planningRepository.countByLocationId(2L)).thenReturn(2L);

        Location result = locationService.findLeastUsedLocation();

        assertEquals(2L, result.getId());
    }

    // ✅ SUGGEST BEST
    @Test
    void shouldSuggestBestLocation() {
        when(locationRepository.findByType(LocationType.ROOM))
                .thenReturn(List.of(loc1));

        Location result = locationService.suggestBestLocation(20, LocationType.ROOM);

        assertEquals(loc1, result);
    }

    // ❌ NO SUITABLE LOCATION
    @Test
    void shouldThrowWhenNoSuitableLocation() {
        when(locationRepository.findByType(LocationType.ROOM))
                .thenReturn(List.of());

        assertThrows(RuntimeException.class,
                () -> locationService.suggestBestLocation(100, LocationType.ROOM));
    }

    // ✅ OVERLOADED
    @Test
    void shouldFindOverloadedLocations() {
        when(locationRepository.findAll()).thenReturn(List.of(loc1));
        when(planningRepository.countByLocationId(1L)).thenReturn(20L);

        List<Location> result = locationService.findOverloadedLocations(10);

        assertEquals(1, result.size());
    }

    // ✅ SEARCH
    @Test
    void shouldSearchLocations() {
        when(locationRepository.findAll()).thenReturn(List.of(loc1));

        List<Location> result = locationService.searchLocations("room");

        assertEquals(1, result.size());
    }

    // ✅ ONLINE LOCATIONS
    @Test
    void shouldGetOnlineLocations() {
        when(locationRepository.findByType(LocationType.ONLINE_PLATFORM))
                .thenReturn(List.of(loc2));

        List<Location> result = locationService.getOnlineLocations();

        assertEquals(1, result.size());
    }

    // ✅ VALID LOCATION (ONLINE)
    @Test
    void shouldValidateOnlineLocation() {
        boolean result = locationService.isValidLocation(loc2);

        assertTrue(result);
    }

    // ❌ INVALID LOCATION
    @Test
    void shouldInvalidateLocation() {
        Location invalid = new Location();
        invalid.setType(LocationType.ROOM);
        invalid.setCapacity(0);

        boolean result = locationService.isValidLocation(invalid);

        assertFalse(result);
    }
}
