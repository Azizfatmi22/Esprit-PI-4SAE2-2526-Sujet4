// clients/CourseManagementFeignClient.java
package com.mspathandschedule.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(
        name = "MS-COURSE",
        url = "${api.gateway.url:http://localhost:8085}",
        configuration = FeignConfig.class// Use API Gateway URL
)
public interface CourseManagementFeignClient {

    // Get all courses (paginated - returns Page object)
    @GetMapping("/api/courses")
    Map<String, Object> getAllCourses(@RequestParam("page") int page,
                                      @RequestParam("size") int size);

    // Get courses by level - RETURNS List<CourseSummaryDTO>
    @GetMapping("/api/courses/level/{level}")
    List<Map<String, Object>> getCoursesByLevel(@PathVariable("level") String level);

    // Search courses by keyword (title or description)
    @GetMapping("/api/courses/search")
    List<Map<String, Object>> searchCourses(@RequestParam("keyword") String keyword);

    // Get course by ID with full details
    @GetMapping("/api/courses/{id}")
    Map<String, Object> getCourseById(@PathVariable("id") Long id);

    // Get courses by trainer
    @GetMapping("/api/courses/trainer/{trainerId}")
    List<Map<String, Object>> getCoursesByTrainer(@PathVariable("trainerId") String trainerId);

    // Get top rated courses
    @GetMapping("/api/courses/top-rated")
    List<Map<String, Object>> getTopRatedCourses(@RequestParam("limit") int limit);
}