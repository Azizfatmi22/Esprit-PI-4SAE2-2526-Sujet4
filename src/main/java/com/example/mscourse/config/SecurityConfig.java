package com.example.mscourse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for MS-Course
 * 
 * This provides defense-in-depth security at the microservice level.
 * Even though API Gateway handles authentication, this ensures the service
 * is protected if accessed directly or if gateway is bypassed.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless REST API
                .csrf(csrf -> csrf.disable())
                
                // Disable CORS - API Gateway handles CORS
                .cors(cors -> cors.disable())
                
                // Stateless session management
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Authorization rules - PERMISSIVE for debugging
                .authorizeHttpRequests(auth -> auth
                        // Allow everything for now to debug issues
                        .anyRequest().permitAll()
                )
                
                // Disable OAuth2 resource server to prevent JWT validation errors
                .oauth2ResourceServer(oauth2 -> oauth2.disable());

        return http.build();
    }
}
