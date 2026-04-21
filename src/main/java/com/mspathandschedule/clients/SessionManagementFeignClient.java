// clients/SessionManagementFeignClient.java
package com.mspathandschedule.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(
        name = "MS-SESSIONMANGEMENT",
        url = "${api.gateway.url:http://localhost:8085}" ,
        configuration = FeignConfig.class// Use API Gateway URL
)
public interface SessionManagementFeignClient {

    // Get session by ID - GET /api/sessions/{id}
    @GetMapping("/api/sessions/{id}")
    Map<String, Object> getSessionById(@PathVariable("id") Long id);

    // Get sessions by user and course - GET /api/sessions/by-user-course
    @GetMapping("/api/sessions/by-user-course")
    List<Map<String, Object>> getSessionsByUserAndCourse(
            @RequestParam("userId") String userId,
            @RequestParam("courseId") Long courseId);

    // Get all sessions - GET /api/sessions
    @GetMapping("/api/sessions")
    List<Map<String, Object>> getAllSessions();

    // Get sessions by trainer - GET /api/sessions/trainer
    @GetMapping("/api/sessions/trainer")
    List<Map<String, Object>> getSessionsByTrainer(@RequestParam("trainerId") String trainerId);

    // Check trainer availability - GET /api/sessions/check-trainer-availability
    @GetMapping("/api/sessions/check-trainer-availability")
    Boolean checkTrainerAvailability(
            @RequestParam("trainerId") String trainerId,
            @RequestParam("date") String date);

    // Check trainer overload - GET /api/sessions/check-trainer-overload
    @GetMapping("/api/sessions/check-trainer-overload")
    Boolean checkTrainerOverload(
            @RequestParam("trainerId") String trainerId,
            @RequestParam("date") String date);

    @GetMapping("/api/plannings/{id}")
    Map<String, Object> getPlanningById(@PathVariable("id") Long id);

    // Get multiple plannings by IDs (batch)
    @PostMapping("/api/plannings/batch")
    List<Map<String, Object>> getPlanningsByIds(@RequestBody List<Long> ids);
}