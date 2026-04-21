package tn.esprit.microservice.reclamation.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.microservice.reclamation.entities.ModerationResult;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.entities.ReclamationType;
import tn.esprit.microservice.reclamation.repositories.ReclamationRepository;
import tn.esprit.microservice.reclamation.services.interfaces.IGravityCalculationService;
import tn.esprit.microservice.reclamation.services.interfaces.IModerationService;
import tn.esprit.microservice.reclamation.services.interfaces.IReclamationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReclamationServiceImpl implements IReclamationService {

    @Autowired
    private ReclamationRepository reclamationRepository;

    @Autowired
    private IModerationService moderationService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private IGravityCalculationService gravityService;

    @Value("${app.admin.email}")
    private String adminEmail;

    // ==============================================================
    // UNE SEULE méthode createReclamation (pas deux !)
    // ==============================================================
    @Override
    @Transactional
    public Reclamation createReclamation(Reclamation reclamation) {
        reclamation.setCreatedDate(LocalDateTime.now());
        reclamation.setStatus(ReclamationStatus.PENDING);

        // 🔍 SCAN DE MODÉRATION (existant)
        ModerationResult moderation = moderationService.scanReclamation(reclamation);
        if (moderation.isSuspect()) {
            reclamation.setIsSuspect(true);
            reclamation.setModerationReason(moderation.getReason());
            sendModerationAlert(reclamation, moderation.getDetectedWords());
        }

        // ⭐ CALCUL DE GRAVITÉ (NOUVEAU)
        GravityCalculationService.GravityResult gravity = gravityService.calculateGravity(reclamation);
        reclamation.setGravityScore(gravity.getScore());
        reclamation.setGravityLevel(gravity.getLevel().toString());

        return reclamationRepository.save(reclamation);
    }

    @Override
    public Reclamation getReclamationById(Long id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée avec l'ID : " + id));
    }

    @Override
    public List<Reclamation> getAllReclamations() {
        return reclamationRepository.findAll();
    }

    @Override
    public List<Reclamation> getReclamationsByLearner(String learnerId) {
        return reclamationRepository.findByLearnerId(learnerId);
    }

    @Override
    public List<Reclamation> getReclamationsByStatus(ReclamationStatus status) {
        return reclamationRepository.findByStatus(status);
    }

    @Override
    public List<Reclamation> getReclamationsByType(ReclamationType type) {
        return reclamationRepository.findByType(type);
    }

    @Override
    @Transactional
    public Reclamation updateReclamationStatus(Long id, ReclamationStatus status, Long adminId) {
        Reclamation reclamation = getReclamationById(id);
        reclamation.setStatus(status);
        reclamation.setUpdatedDate(LocalDateTime.now());

        if (adminId != null) {
            reclamation.setAdminId(adminId);
        }

        if (status == ReclamationStatus.RESOLVED && reclamation.getResolvedDate() == null) {
            reclamation.setResolvedDate(LocalDateTime.now());
        }

        return reclamationRepository.save(reclamation);
    }

    @Override
    @Transactional
    public Reclamation updateReclamation(Long id, Reclamation reclamation) {
        Reclamation existing = getReclamationById(id);

        if (reclamation.getSubject() != null) {
            existing.setSubject(reclamation.getSubject());
        }
        if (reclamation.getDescription() != null) {
            existing.setDescription(reclamation.getDescription());
        }
        if (reclamation.getType() != null) {
            existing.setType(reclamation.getType());
        }
        if (reclamation.getCourseId() != null) {
            existing.setCourseId(reclamation.getCourseId());
        }
        if (reclamation.getPriority() != null) {
            existing.setPriority(reclamation.getPriority());
        }

        existing.setUpdatedDate(LocalDateTime.now());

        return reclamationRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteReclamation(Long id) {
        Reclamation reclamation = getReclamationById(id);
        reclamationRepository.delete(reclamation);
    }

    @Override
    public List<Reclamation> getUnresolvedReclamations() {
        return reclamationRepository.findByStatusNot(ReclamationStatus.RESOLVED);
    }

    @Override
    @Transactional
    public Reclamation submitSatisfaction(Long id, Integer score, String comment) {
        Reclamation rec = getReclamationById(id);
        rec.setSatisfactionScore(score);
        rec.setSatisfactionComment(comment);
        rec.setSatisfactionDate(LocalDateTime.now());
        return reclamationRepository.save(rec);
    }

    // ==============================================================
    // Méthode privée pour l'alerte email
    // ==============================================================
    private void sendModerationAlert(Reclamation reclamation, List<String> words) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(adminEmail);
            email.setSubject("🚨 ALERTE MODÉRATION - Réclamation suspecte #" + reclamation.getId());
            email.setText(String.format(
                    "Une réclamation a été marquée comme suspecte.\n\n" +
                            "ID: #%d\n" +
                            "Apprenant: #%d\n" +
                            "Mots détectés: %s\n\n" +
                            "Sujet: %s\n\n" +
                            "Description: %s\n\n" +
                            "Connectez-vous pour modérer.",
                    reclamation.getId(),
                    reclamation.getLearnerId(),
                    String.join(", ", words),
                    reclamation.getSubject(),
                    reclamation.getDescription()
            ));
            mailSender.send(email);
        } catch (Exception e) {
            System.err.println("Erreur envoi email modération: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Reclamation approveReclamation(Long id) {
        Reclamation reclamation = getReclamationById(id);
        reclamation.setIsSuspect(false);
        reclamation.setModerationReason(null);
        reclamation.setModeratedDate(LocalDateTime.now());
        return reclamationRepository.save(reclamation);
    }

    @Override
    @Transactional
    public Reclamation rejectReclamation(Long id) {
        Reclamation reclamation = getReclamationById(id);
        reclamation.setIsSuspect(false);
        reclamation.setStatus(ReclamationStatus.REJECTED);
        reclamation.setModeratedDate(LocalDateTime.now());
        return reclamationRepository.save(reclamation);
    }


}