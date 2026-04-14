package com.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(csrf -> csrf.disable())

                // ✅ VERY IMPORTANT: Enable CORS inside security
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                )
                .authorizeExchange(exchanges -> exchanges

                        // ✅ allow preflight requests (CRITICAL for CORS)
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        // public routes
                        .pathMatchers("/public/**").permitAll()

                        // restricted routes
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/courses/uploads/**").permitAll()

                        // everything else requires authentication
                        .anyExchange().authenticated()
                )

                // ✅ Keycloak JWT validation
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                )

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // ✅ your Angular app
        config.setAllowedOrigins(Arrays.asList("http://localhost:4200"));

        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(Arrays.asList("*"));

        config.setAllowCredentials(true);

        // ✅ VERY IMPORTANT for JWT / Authorization header
        config.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}