package com.formini.msliveclass.dto;

public class ChatbotResponse {
    private String response;
    private String timestamp;

    public ChatbotResponse() {}

    public ChatbotResponse(String response, String timestamp) {
        this.response = response;
        this.timestamp = timestamp;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
