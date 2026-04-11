package com.mspathandschedule.services.interfaces;

import com.mspathandschedule.entities.LearningPath;

import java.util.List;

public interface LearningPathService {

    // CRUD
    LearningPath createLearningPath(LearningPath lp);
    LearningPath updateLearningPath(Long id, LearningPath lp);
    LearningPath getLearningPath(Long id);
    List<LearningPath> getAllLearningPaths();
    void deleteLearningPath(Long id);

    // Session management
    LearningPath addSessionToPath(Long pathId, Long sessionId);
    LearningPath removeSessionFromPath(Long pathId, Long sessionId);




}