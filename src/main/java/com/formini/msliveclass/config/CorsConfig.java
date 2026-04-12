package com.formini.msliveclass.config;

import org.springframework.context.annotation.Configuration;

/**
 * CORS Configuration
 * 
 * NOTE: CORS is handled at the API Gateway level.
 * This microservice should NOT configure CORS to avoid duplicate headers.
 * 
 * If you need to test this microservice directly (without the gateway),
 * uncomment the CorsFilter bean below.
 */
@Configuration
public class CorsConfig {

    // CORS is handled by API Gateway - do not configure here
    // Uncomment only for direct testing without gateway
    
    /*
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        config.addAllowedHeader("*");
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
    */
}
