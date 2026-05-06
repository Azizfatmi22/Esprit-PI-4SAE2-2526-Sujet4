package com.sessionmanagementservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sessionmanagementservice.Services.interfaces.PlanningService;
import com.sessionmanagementservice.entities.Planning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanningController.class)
@ContextConfiguration(classes = {PlanningControllerTest.TestSecurityConfig.class, PlanningController.class})
class PlanningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanningService planningService;

    @Autowired
    private ObjectMapper objectMapper;

    @Configuration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll()
                    );
            return http.build();
        }
    }

    private Planning createPlanning() {
        Planning p = new Planning();
        p.setId((long)1); // ✅ FIX: Added missing ID
        p.setStartDate(LocalDate.now().plusDays(1));
        p.setEndDate(LocalDate.now().plusDays(3));
        p.setTotalHours(35);
        return p;
    }

    // ✅ CREATE
    @Test
    void shouldCreatePlanning() throws Exception {
        Planning planning = createPlanning();

        when(planningService.createPlanning(any(), eq(1L), eq(1L)))
                .thenReturn(planning);

        mockMvc.perform(post("/api/plannings")
                        .param("sessionId", "1")
                        .param("locationId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planning)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHours").value(35));
    }

    // ✅ GET BY ID
    @Test
    void shouldGetById() throws Exception {
        Planning planning = createPlanning();

        when(planningService.getPlanningById(1)).thenReturn(planning);

        mockMvc.perform(get("/api/plannings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ✅ GET BY SESSION
    @Test
    void shouldGetBySession() throws Exception {
        when(planningService.getPlanningsBySession(1L))
                .thenReturn(List.of(createPlanning()));

        mockMvc.perform(get("/api/plannings/session/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    // ✅ UPDATE
    @Test
    void shouldUpdatePlanning() throws Exception {
        Planning planning = createPlanning();

        when(planningService.updatePlanning(eq(1), any()))
                .thenReturn(planning);

        mockMvc.perform(put("/api/plannings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planning)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ✅ DELETE
    @Test
    void shouldDeletePlanning() throws Exception {
        doNothing().when(planningService).deletePlanning(1);

        mockMvc.perform(delete("/api/plannings/1"))
                .andExpect(status().isNoContent());
    }

    // ✅ GENERATE
    @Test
    void shouldGeneratePlanning() throws Exception {
        when(planningService.generatePlanning(1L))
                .thenReturn(createPlanning());

        mockMvc.perform(post("/api/plannings/generate")
                        .param("sessionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ✅ DISTRIBUTE
    @Test
    void shouldDistributePlanning() throws Exception {
        when(planningService.distributePlanning(1L, 1L, 5))
                .thenReturn(createPlanning());

        mockMvc.perform(post("/api/plannings/distribute")
                        .param("sessionId", "1")
                        .param("locationId", "1")
                        .param("numberOfDays", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ✅ CONFLICT
    @Test
    void shouldCheckConflict() throws Exception {
        when(planningService.hasPlanningConflict(any(), any(), any()))
                .thenReturn(true);

        mockMvc.perform(get("/api/plannings/conflict")
                        .param("locationId", "1")
                        .param("startDate", "2026-05-10")
                        .param("endDate", "2026-05-12"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // ✅ SUGGEST DATE - FIXED
    @Test
    void shouldSuggestDate() throws Exception {
        when(planningService.suggestNextAvailableDate(any(), any()))
                .thenReturn(LocalDate.of(2026, 5, 10));

        mockMvc.perform(get("/api/plannings/suggest-date")
                        .param("locationId", "1")
                        .param("startDate", "2026-05-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("2026-05-10"));  // ✅ FIX: Use jsonPath instead of content().string()
    }

    // ✅ COUNT BY LOCATION
    @Test
    void shouldCountByLocation() throws Exception {
        when(planningService.countPlanningsByLocation(1L))
                .thenReturn(5L);

        mockMvc.perform(get("/api/plannings/count-by-location")
                        .param("locationId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    // ✅ HIGH RISK
    @Test
    void shouldCheckHighRisk() throws Exception {
        when(planningService.isHighRiskPlanning(1L))
                .thenReturn(Map.of("riskScore", 70, "isHighRisk", true));

        mockMvc.perform(get("/api/plannings/high-risk")
                        .param("sessionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHighRisk").value(true));
    }

    // ✅ SMART DATE - FIXED
    @Test
    void shouldSmartSuggestDate() throws Exception {
        when(planningService.smartSuggestDate(any(), any()))
                .thenReturn(LocalDate.of(2026, 5, 15));

        mockMvc.perform(get("/api/plannings/smart-date")
                        .param("locationId", "1")
                        .param("startDate", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("2026-05-15"));  // ✅ FIX: Use jsonPath instead of content().string()
    }
}