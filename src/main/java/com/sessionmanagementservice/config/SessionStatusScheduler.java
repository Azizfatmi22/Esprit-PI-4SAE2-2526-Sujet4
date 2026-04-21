package com.sessionmanagementservice.config;

import com.sessionmanagementservice.Services.interfaces.SessionService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SessionStatusScheduler {

    private final SessionService sessionService;

    public SessionStatusScheduler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Vérifie et met à jour le statut des sessions toutes les heures
     * (entre 8h et 20h)
     */
    @Scheduled(cron = "0 0 8-20 * * *")
    public void updateSessionStatus() {
        System.out.println("Vérification et mise à jour des statuts de session...");
        sessionService.updateSessionsStatusBasedOnPlanning();
    }

    /**
     * Vérification complète une fois par jour à minuit
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void fullDailyUpdate() {
        System.out.println("Mise à jour quotidienne des statuts de session...");
        sessionService.getAllSessionsWithCurrentStatus();
    }
}
