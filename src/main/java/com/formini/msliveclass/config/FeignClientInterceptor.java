package com.formini.msliveclass.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Feign interceptor - Security disabled
 */
@Component  
public class FeignClientInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignClientInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        // No security - just log the request
        log.debug("Feign request to: {}", template.url());
    }
}
