package tn.esprit.mucroservice.msenrollment.Scheduler;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.*;
import tn.esprit.mucroservice.msenrollment.services.interfaces.INotificationService;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class NotificationScheduler {

    @Autowired private INotificationService notificationService;
    @Autowired private CartRepository cartRepository;
    @Autowired private InstallmentRepository installmentRepository;

    // ===== 1. PANIER ABANDONNÉ — toutes les heures =====
    @Scheduled(fixedRate = 3000) // 1h
    @Transactional
    public void checkAbandonedCarts() {
        System.out.println("=== Vérification paniers abandonnés ===");

        //Date cutoff = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000); // -24h
        Date cutoff = new Date(System.currentTimeMillis() - 3000);
        List<Cart> abandonedCarts = cartRepository.findCartsOlderThan(cutoff);

        for (Cart cart : abandonedCarts) {
            if (cart.getItems() != null && !cart.getItems().isEmpty()) {
                notificationService.notifyCartAbandoned(
                        cart.getLearnerId(),
                        cart.getItems().size()
                );
            }
        }
    }

    // ===== 2. RAPPEL ÉCHÉANCES (3 jours avant) — chaque jour à 9h =====
    @Scheduled(cron = "0 0 9 * * ?")
    public void remindUpcomingInstallments() {
        System.out.println("=== Rappel échéances à venir ===");

        // Chercher les échéances dues dans 3 jours
        Date in3Days = new Date(System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000);
        Date tomorrow = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);

        List<Installment> upcoming = installmentRepository
                .findByDueDateBetweenAndStatus(tomorrow, in3Days, InstallmentStatus.PENDING);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (Installment inst : upcoming) {
            InstallmentPlan plan = inst.getInstallmentPlan();
            notificationService.notifyInstallmentReminder(
                    plan.getLearnerId(),
                    inst.getAmount(),
                    sdf.format(inst.getDueDate()),
                    inst.getInstallmentNumber(),
                    inst.getId()
            );
        }
    }

    // ===== 3. ÉCHÉANCES EN RETARD — chaque jour à minuit =====
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkOverdueAndNotify() {
        System.out.println("=== Vérification retards et notifications ===");

        List<Installment> overdue = installmentRepository
                .findOverdueInstallments(new Date(), InstallmentStatus.PENDING);

        for (Installment inst : overdue) {
            inst.setStatus(InstallmentStatus.OVERDUE);
            InstallmentPlan plan = inst.getInstallmentPlan();
            if (plan.getStatus() == InstallmentPlanStatus.ACTIVE) {
                plan.setStatus(InstallmentPlanStatus.DEFAULTED);
            }
            notificationService.notifyInstallmentOverdue(
                    plan.getLearnerId(),
                    inst.getAmount(),
                    inst.getInstallmentNumber(),
                    inst.getId()
            );
        }
    }
}