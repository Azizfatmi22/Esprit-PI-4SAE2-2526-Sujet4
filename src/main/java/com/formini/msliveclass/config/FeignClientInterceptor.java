package com.formini.msliveclass.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Feign interceptor to forward JWT token to other microservices
 */
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignClientInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String token = jwt.getTokenValue();
                
                // Add Authorization header with Bearer token
                template.header("Authorization", "Bearer " + token);
                log.debug("Added Authorization header to Feign request: {}", template.url());
            } else {
                log.warn("No JWT token found in security context for Feign request: {}", template.url());
            }
        } catch (Exception e) {
            log.error("Error adding Authorization header to Feign request", e);
        }
    }
}
