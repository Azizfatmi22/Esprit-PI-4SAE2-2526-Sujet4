package com.formini.msliveclass.dto;

public class ChatbotRequest {
    private String message;
    private String userId; // Deprecated, use learnerId
    private String learnerId; // The authenticated learner's ID
    private String learnerName; // The authenticated learner's name

    public ChatbotRequest() {}

    public ChatbotRequest(String message, String learnerId) {
        this.message = message;
        this.learnerId = learnerId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId != null ? userId : learnerId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLearnerId() {
        return learnerId != null ? learnerId : userId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public String getLearnerName() {
        return learnerName;
    }

    public void setLearnerName(String learnerName) {
        this.learnerName = learnerName;
    }
}
