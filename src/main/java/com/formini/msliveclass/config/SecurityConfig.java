package com.formini.msliveclass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable()) // Disable CORS - API Gateway handles it
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/public/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS).permitAll() // CORS preflight
                .requestMatchers("/actuator/**", "/health", "/info").permitAll() // Monitoring endpoints
                
                // Temporarily allow all live session endpoints to debug 404 issue
                .requestMatchers("/api/livesession/**").permitAll()
                .requestMatchers("/api/chat/**").permitAll()
                .requestMatchers("/api/poll/**").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}
