package com.example.msforum.backoffice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backoffice/events/analytics")
public class EventAnalyticsController {

    private final EventAnalyticsService analyticsService;

    public EventAnalyticsController(EventAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public EventAnalyticsDTO getAnalytics() {
        return analyticsService.getAnalytics();
    }
}
