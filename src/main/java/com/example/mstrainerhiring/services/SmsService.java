package com.example.mstrainerhiring.services;

public interface SmsService {
    void sendApplicationReceivedSms(String phoneNumber);

    void sendApplicationRejectedSms(String phoneNumber, String reasoning);

    void sendApplicationStatusUpdateSms(String phoneNumber, String status, String customText);
}
