package com.example.mstrainerhiring.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class TwilioConfig {

    @Value("${twilio.account_sid}")
    private String accountSid;

    @Value("${twilio.auth_token}")
    private String authToken;

    @PostConstruct
    public void initTwilio() {
        if (!"AC_PLACEHOLDER".equals(accountSid) && !accountSid.isEmpty()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio API initialized successfully.");
        } else {
            log.warn("Twilio API credentials not configured (using placeholders). SMS features will not work.");
        }
    }
}
