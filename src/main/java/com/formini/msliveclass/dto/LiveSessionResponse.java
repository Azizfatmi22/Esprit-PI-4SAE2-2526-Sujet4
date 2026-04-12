package com.formini.msliveclass.dto;

import com.formini.msliveclass.entities.LiveSession;

public class LiveSessionResponse {

    private LiveSession liveSession;
    private String courseTitle;

    public LiveSessionResponse() {
    }

    public LiveSessionResponse(LiveSession liveSession, String courseTitle) {
        this.liveSession = liveSession;
        this.courseTitle = courseTitle;
    }

    public LiveSession getLiveSession() {
        return liveSession;
    }

    public void setLiveSession(LiveSession liveSession) {
        this.liveSession = liveSession;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }
}
