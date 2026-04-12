package com.formini.msliveclass.clients;

import com.formini.msliveclass.dto.EnrollmentSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "MS-Enrollment")
public interface EnrollmentClient {

    @GetMapping("/msenrollment/enrollments/learner/{learnerId}")
    List<EnrollmentSummary> getEnrollmentsByLearner(@PathVariable("learnerId") String learnerId);
    
    @GetMapping("/msenrollment/enrollments/course/{courseId}")
    List<EnrollmentSummary> getEnrollmentsByCourse(@PathVariable("courseId") Long courseId);
    
    @GetMapping("/msenrollment/installments/access/{learnerId}/{courseId}")
    Boolean hasCourseAccess(@PathVariable("learnerId") String learnerId, @PathVariable("courseId") Long courseId);
    
    @GetMapping("/msenrollment/enrollments/all")
    List<Map<String, Object>> getAllEnrollments();
    
    @GetMapping("/msenrollment/enrollments/learner/{learnerId}")
    List<Map<String, Object>> getEnrollmentsByLearnerId(@PathVariable("learnerId") String learnerId);
}
