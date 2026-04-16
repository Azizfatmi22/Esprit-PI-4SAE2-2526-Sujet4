package com.example.mstrainerhiring.services.impl;

import com.example.mstrainerhiring.services.SmsService;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Value("${twilio.messaging_service_sid}")
    private String messagingServiceSid;

    @Value("${twilio.account_sid}")
    private String accountSid;

    @Override
    @Async
    public void sendApplicationReceivedSms(String phoneNumber) {
        String body = "Hi there! Your application to become a trainer has been successfully received. We will notify you once it has been reviewed.";
        sendSms(phoneNumber, body);
    }

    @Override
    @Async
    public void sendApplicationRejectedSms(String phoneNumber, String reasoning) {
        String body = "Dear applicant, unfortunately, your application has been automatically rejected. Reason: "
                + reasoning;
        sendSms(phoneNumber, body);
    }

    @Override
    @Async
    public void sendApplicationStatusUpdateSms(String phoneNumber, String status, String customText) {
        String body = "Update on your Trainer Application: Your status is now [" + status + "]. " +
                (customText != null && !customText.isEmpty() ? customText : "");
        sendSms(phoneNumber, body);
    }

    private void sendSms(String toPhoneNumber, String body) {
        if ("AC_PLACEHOLDER".equals(accountSid) || accountSid.isEmpty()) {
            log.warn("Twilio not configured. Would have sent SMS to {}: {}", toPhoneNumber, body);
            return;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    messagingServiceSid,
                    body).create();
            log.info("SMS sent successfully to {}! SID: {}", toPhoneNumber, message.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage());
        }
    }
}
