package com.formini.msliveclass.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class EnrollmentSummary {

    private Long id;
    private String learnerId;

    @JsonAlias({"courseId", "CourseId"})
    private Long courseId;

    private String status;

    public EnrollmentSummary() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
