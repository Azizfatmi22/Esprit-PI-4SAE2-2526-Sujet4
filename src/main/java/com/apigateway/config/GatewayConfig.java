package com.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Course Service Routes
                .route("ms-course-route", r -> r
                        .path("/api/courses/**")
                        .uri("lb://MS-COURSE"))

                // File Upload Routes
                .route("ms-course-uploads-route", r -> r
                        .path("/api/files/**")
                        .filters(f -> f.rewritePath("/api/files/(?<segment>.*)", "/api/courses/uploads/${segment}"))
                        .uri("lb://MS-COURSE"))
                //LearningPath & schedule MS
                .route("learning-path-service", r -> r
                        .path("/api/learning-paths/**")
                        .uri("lb://MSPathAndSchedule"))

                .route("schedule-service", r -> r
                        .path("/api/schedules/**")
                        .uri("lb://MSPathAndSchedule"))
                // Session Management Service Routes
                .route("session-service", r -> r
                        .path("/api/sessions/**")
                        .uri("lb://MS-SESSIONMANGEMENT"))
                .route("planning-service", r -> r
                        .path("/api/plannings/**")
                        .uri("lb://MS-SESSIONMANGEMENT"))
                .route("location-service", r -> r
                        .path("/api/locations/**")
                        .uri("lb://MS-SESSIONMANGEMENT"))
                // Evaluation Service Routes
                .route("evaluation-service", r -> r
                        .path("/evaluations/**", "/exam/answers/**", "/api/quiz-answers/**")
                        .uri("lb://MS-EVALUATION"))

                // ✅ Enrollment Service Routes (ADDED)
                .route("enrollment-service", r -> r
                        .path("/msenrollment/**")
                        .uri("lb://MS-ENROLLMENT"))

                // Forum Service Routes
                .route("forum-service", r -> r
                        .path("/api/posts/**")
                        .uri("lb://FORUM-SERVICE"))
                .route("forum-event-service", r -> r
                        .path("/api/events/**")
                        .uri("lb://FORUM-SERVICE"))
                .route("forum-moderation-service", r -> r
                        .path("/api/moderation/**")
                        .uri("lb://FORUM-SERVICE"))
                .route("forum-reputation-service", r -> r
                        .path("/api/reputation/**")
                        .uri("lb://FORUM-SERVICE"))
                .route("forum-image-service", r -> r
                        .path("/api/images/**")
                        .uri("lb://FORUM-SERVICE"))
                .route("forum-analytics-service", r -> r
                        .path("/api/backoffice/events/analytics/**")
                        .uri("lb://FORUM-SERVICE"))
                .route("forum-backoffice-service", r -> r
                        .path("/api/backoffice/**")
                        .uri("lb://FORUM-SERVICE"))

                // MS-CourseProgress Routes
                .route("course-progress-service", r -> r
                        .path("/progress/**")
                        .uri("lb://MS-COURSEPROGRESS"))

                // MS-Certificate Routes
                .route("certificate-service", r -> r
                        .path("/api/certificates/**")
                        .uri("lb://MS-CERTIFICATE"))

                // MS-TrainerHiring Routes
                .route("trainer-hiring-service", r -> r
                        .path("/api/partners/**", "/api/trainers/**", "/api/jobs/**")
                        .uri("lb://MS-TRAINERHIRING"))

                .route("recommendation-service", r -> r
                        .path("/api/recommend/**")
                        .uri("http://localhost:5000"))
                .route("ms-reporting-route", r -> r
                        .path("/reporting/**")
                        .uri("lb://MS-REPORTING-CERTIFICATION"))

                // ========================================
                // MS-LIVECLASS Routes
                // ========================================
                .route("livesession-route", r -> r
                        .path("/api/livesession/**")
                        .uri("lb://MS-LIVECLASS"))

                .route("poll-route", r -> r
                        .path("/api/poll/**")
                        .uri("lb://MS-LIVECLASS"))

                .route("chat-route", r -> r
                        .path("/api/chat/**")
                        .uri("lb://MS-LIVECLASS"))

                .route("chatbot-route", r -> r
                        .path("/api/chatbot/**")
                        .uri("lb://MS-LIVECLASS"))

                .build();
    }

}
