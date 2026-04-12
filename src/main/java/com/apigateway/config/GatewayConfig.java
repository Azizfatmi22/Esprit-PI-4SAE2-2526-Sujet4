package com.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
                
                // Session Management Service Routes
                .route("session-service", r -> r
                        .path("/api/sessions/**")
                        .uri("lb://MS-SESSIONMANGEMENT"))
                .route("planning-service", r -> r
                        .path("/api/plannings/**")
                        .uri("lb://MS-SESSIONMANGEMENT"))
                .route("location-service", r -> r
                        .path("/api/location/**")
                        .uri("lb://MS-SESSIONMANGEMENT"))
                // Evaluation Service Routes
                .route("evaluation-service", r -> r
                        .path("/evaluations/**", "/exam/answers/**", "/api/quiz-answers/**")
                        .uri("lb://MS-EVALUATION"))
                .route("reporting-service", r -> r
                        .path("/reporting/**")
                        .uri("lb://MS-REPORTING-CERTIFICATION"))

                // ✅ Enrollment Service Routes (ADDED)
                .route("enrollment-service", r -> r
                        .path("/msenrollment/**")
                        .uri("lb://MS-ENROLLMENT"))

                .build();
    }
}
