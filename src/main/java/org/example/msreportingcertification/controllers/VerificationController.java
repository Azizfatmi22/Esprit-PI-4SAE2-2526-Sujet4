package org.example.msreportingcertification.controllers;

import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.repositories.EvaluationHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/verify")
@CrossOrigin("*")
public class VerificationController {
    @Autowired
    private EvaluationHistoryRepository repository;

    @GetMapping("/{id}")
    public String verifyCertificate(@PathVariable Long id) {
        return repository.findById(id)
                .map(this::generateSuccessHtml)
                .orElse("<html><body style='text-align:center; font-family:sans-serif; padding-top:50px;'>" +
                        "<h1 style='color:red;'>❌ Certificat Invalide</h1>" +
                        "<p>Ce certificat n'existe pas dans notre base de données.</p>" +
                        "</body></html>");
    }

    private String generateSuccessHtml(EvaluationHistory history) {
        String statusColor = history.getIsPassed() ? "#10b981" : "#ef4444";
        String statusText = history.getIsPassed() ? "VALIDE" : "ÉCHOUÉ";

        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Vérification Formini</title>
            <style>
                body { font-family: 'Segoe UI', sans-serif; background-color: #f1f5f9; margin: 0; display: flex; justify-content: center; align-items: center; min-height: 100vh; }
                .card { background: white; padding: 2rem; border-radius: 15px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); width: 90%; max-width: 400px; text-align: center; border-top: 8px solid [COLOR]; }
                .badge { background: [COLOR]; color: white; padding: 5px 15px; border-radius: 20px; font-weight: bold; font-size: 0.9rem; }
                h1 { color: #1e293b; margin: 15px 0; font-size: 1.5rem; }
                .info { text-align: left; margin: 20px 0; background: #f8fafc; padding: 15px; border-radius: 8px; }
                .label { color: #64748b; font-size: 0.8rem; text-transform: uppercase; }
                .value { color: #0f172a; font-weight: bold; margin-bottom: 10px; display: block; }
                .footer { color: #94a3b8; font-size: 0.8rem; margin-top: 20px; }
            </style>
        </head>
        <body>
            <div class="card">
                <span class="badge">[STATUS]</span>
                <h1>Vérification Réussie</h1>
                <div class="info">
                    <span class="label">Apprenant</span>
                    <span class="value">[NAME]</span>
                    <span class="label">Évaluation</span>
                    <span class="value">[TITLE]</span>
                    <span class="label">Score Obtenu</span>
                    <span class="value">[SCORE]%</span>
                    <span class="label">Date d'émission</span>
                    <span class="value">[DATE]</span>
                </div>
                <div class="footer">Authentifié par le système Formini</div>
            </div>
        </body>
        </html>
        """
                .replace("[COLOR]", statusColor)
                .replace("[STATUS]", statusText)
                .replace("[NAME]", history.getLearnerName())
                .replace("[TITLE]", history.getEvaluationTitle())
                .replace("[SCORE]", String.valueOf(history.getPercentage()))
                .replace("[DATE]", history.getReceivedAt().toLocalDate().toString());
    }
}
