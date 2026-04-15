package com.example.msforum.services;

import com.example.msforum.entities.ContentStatus;

public interface ModerationService {
    ContentStatus moderateContent(String text);
}
