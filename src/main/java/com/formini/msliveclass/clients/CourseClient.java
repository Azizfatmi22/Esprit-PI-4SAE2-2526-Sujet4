package com.formini.msliveclass.clients;

import com.formini.msliveclass.dto.ChapterDTO;
import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// Note: Ensure the "name" matches the application name of MS-Course registered in Eureka.
@FeignClient(name = "MS-COURSE")
public interface CourseClient {

    @GetMapping("/api/courses/{id}/title")
    String getCourseTitle(@PathVariable("id") Long id);

    @GetMapping("/api/courses/{id}")
    CourseDTO getCourseById(@PathVariable("id") Long id);
    
    @GetMapping("/api/courses/{id}/with-chapters")
    CourseDTO getCourseWithChapters(@PathVariable("id") Long id);
    
    @GetMapping("/api/courses")
    PageResponse<CourseDTO> getAllCourses(@RequestParam(defaultValue = "0") int page, 
                                          @RequestParam(defaultValue = "100") int size);

    @GetMapping("/api/courses/{courseId}/chapters")
    List<ChapterDTO> getChaptersByCourse(@PathVariable("courseId") Long courseId);

    @GetMapping("/api/courses/{courseId}/chapters/{chapterId}/with-content")
    ChapterDTO getChapterWithContent(@PathVariable("courseId") Long courseId,
                                     @PathVariable("chapterId") Long chapterId);

    @GetMapping("/api/progress/learner/{learnerId}")
    List<java.util.Map<String, Object>> getLearnerProgress(@PathVariable("learnerId") String learnerId);

}
