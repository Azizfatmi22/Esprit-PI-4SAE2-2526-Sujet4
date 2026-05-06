package com.formini.msliveclass.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfanityFilterServiceTest {

    private ProfanityFilterService profanityFilterService;

    @BeforeEach
    void setUp() {
        profanityFilterService = new ProfanityFilterService();
    }

    @Test
    void isUserBanned_InitiallyFalse() {
        assertFalse(profanityFilterService.isUserBanned("learner1"));
    }

    @Test
    void checkAndBanIfProfane_DetectsProfanity() {
        String message = "This is a shit message";
        String learnerId = "learner1";

        boolean detected = profanityFilterService.checkAndBanIfProfane(message, learnerId);

        assertTrue(detected);
        assertTrue(profanityFilterService.isUserBanned(learnerId));
    }

    @Test
    void checkAndBanIfProfane_NoProfanity() {
        String message = "This is a good message";
        String learnerId = "learner1";

        boolean detected = profanityFilterService.checkAndBanIfProfane(message, learnerId);

        assertFalse(detected);
        assertFalse(profanityFilterService.isUserBanned(learnerId));
    }

    @Test
    void unbanUser_Works() {
        profanityFilterService.checkAndBanIfProfane("bad word shit", "learner1");
        assertTrue(profanityFilterService.isUserBanned("learner1"));

        profanityFilterService.unbanUser("learner1");
        assertFalse(profanityFilterService.isUserBanned("learner1"));
    }

    @Test
    void getBanMessage_ReturnsMessageWhenBanned() {
        profanityFilterService.checkAndBanIfProfane("bad word shit", "learner1");
        String message = profanityFilterService.getBanMessage("learner1");

        assertTrue(message.contains("restriction"));
        assertTrue(message.contains("30 minutes") || message.contains("29 minutes"));
    }
}
