package tn.esprit.mucroservice.msenrollment.Scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IInstallmentService;

@Component
public class InstallmentScheduler {

    @Autowired
    private IInstallmentService installmentService;

    // Exécuté chaque jour à minuit
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkOverdueInstallments() {
        System.out.println("=== Vérification des échéances en retard ===");
        installmentService.checkAndMarkOverdueInstallments();
        System.out.println("=== Vérification terminée ===");
    }
}