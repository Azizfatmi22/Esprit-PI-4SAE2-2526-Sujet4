package com.sessionmanagementservice.controllers;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.sessionmanagementservice.Services.interfaces.LocationService;
import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationService locationService;

    @Autowired
    private ObjectMapper objectMapper;

    private Location location;

    @BeforeEach
    void setUp() {
        location = new Location();
        location.setId(1L);
        location.setName("Test Location");
        location.setCapacity(100);
        location.setType(LocationType.ONLINE_PLATFORM);
    }

    // ✅ CREATE
    @Test
    void shouldCreateLocation() throws Exception {
        Mockito.when(locationService.createLocation(any(Location.class)))
                .thenReturn(location);

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Location"));
    }

    // ✅ GET BY ID
    @Test
    void shouldGetLocationById() throws Exception {
        Mockito.when(locationService.getLocationById(1L))
                .thenReturn(location);

        mockMvc.perform(get("/api/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ✅ GET ALL
    @Test
    void shouldGetAllLocations() throws Exception {
        Mockito.when(locationService.getAllLocations())
                .thenReturn(List.of(location));

        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    // ✅ UPDATE
    @Test
    void shouldUpdateLocation() throws Exception {
        Mockito.when(locationService.updateLocation(eq(1L), any(Location.class)))
                .thenReturn(location);

        mockMvc.perform(put("/api/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Location"));
    }

    // ✅ DELETE
    @Test
    void shouldDeleteLocation() throws Exception {
        Mockito.doNothing().when(locationService).deleteLocation(1L);

        mockMvc.perform(delete("/api/locations/1"))
                .andExpect(status().isOk());
    }

    // ✅ VALIDATE
    @Test
    void shouldValidateLocation() throws Exception {
        Mockito.when(locationService.isValidLocation(any(Location.class)))
                .thenReturn(true);

        mockMvc.perform(post("/api/locations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // ✅ SEARCH
    @Test
    void shouldSearchLocations() throws Exception {
        Mockito.when(locationService.searchLocations("test"))
                .thenReturn(List.of(location));

        mockMvc.perform(get("/api/locations/search")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    // ✅ SUGGEST
    @Test
    void shouldSuggestLocation() throws Exception {
        Mockito.when(locationService.suggestBestLocation(50, LocationType.ONLINE_PLATFORM))
                .thenReturn(location);

        mockMvc.perform(get("/api/locations/suggest")
                        .param("capacity", "50")
                        .param("type", "ONLINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Location"));
    }

    // ✅ AVAILABLE
    @Test
    void shouldGetAvailableLocations() throws Exception {
        Mockito.when(locationService.findAvailableLocations(50))
                .thenReturn(List.of(location));

        mockMvc.perform(get("/api/locations/available")
                        .param("capacity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }
}
