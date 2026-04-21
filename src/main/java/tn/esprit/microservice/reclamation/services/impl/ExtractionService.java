package tn.esprit.microservice.reclamation.services.impl;

// ExtractionService.java

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.*;

@Service
public class ExtractionService {

    // ── MOTIFS REGEX ──────────────────────────────────────────────────────────────

    // Montants: 99 DT, 150.50 DT, 200€, 75 dinar
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("(\\d+(?:[.,]\\d{1,2})?)\\s*(?:DT|dinar|€|EUR|euro|dinars)",
                    Pattern.CASE_INSENSITIVE);

    // ID Transaction: TXN-12345, TRX_ABC123, transaction: XYZ
    private static final Pattern TRANSACTION_PATTERN =
            Pattern.compile("(?:TXN|TRX|transaction|trans)[\\s:-]*([A-Z0-9_-]{5,20})",
                    Pattern.CASE_INSENSITIVE);

    // Codes erreur: ERR-404, code 500, erreur: PAYMENT_FAILED
    private static final Pattern ERROR_CODE_PATTERN =
            Pattern.compile("(?:erreur|error|code)[\\s:-]*([A-Z0-9_-]{3,20})",
                    Pattern.CASE_INSENSITIVE);

    // Emails
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    // Dates: 15/04/2025, 15-04-2025, 15 avril 2025
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})|(\\d{1,2})\\s+(janvier|fevrier|mars|avril|mai|juin|juillet|aout|septembre|octobre|novembre|decembre)\\s+(\\d{4})",
                    Pattern.CASE_INSENSITIVE);

    // Numéro facture: INV-12345, FACTURE-ABC
    private static final Pattern INVOICE_PATTERN =
            Pattern.compile("(?:INV|FACTURE|invoice)[\\s:-]*([A-Z0-9_-]{5,20})",
                    Pattern.CASE_INSENSITIVE);

    // Méthode principale d'extraction
    public ExtractedData extract(String text) {
        if (text == null || text.isEmpty()) {
            return new ExtractedData();
        }

        ExtractedData data = new ExtractedData();

        // Extraire les montants
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(text);
        if (amountMatcher.find()) {
            String amountStr = amountMatcher.group(1).replace(',', '.');
            try {
                data.setAmount(Double.parseDouble(amountStr));
            } catch (NumberFormatException e) {}
        }

        // Extraire l'ID de transaction
        Matcher transMatcher = TRANSACTION_PATTERN.matcher(text);
        if (transMatcher.find()) {
            data.setTransactionId(transMatcher.group(1));
        }

        // Extraire le code erreur
        Matcher errorMatcher = ERROR_CODE_PATTERN.matcher(text);
        if (errorMatcher.find()) {
            data.setErrorCode(errorMatcher.group(1));
        }

        // Extraire l'email
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        if (emailMatcher.find()) {
            data.setEmail(emailMatcher.group());
        }

        // Extraire la date
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            String dateStr = extractDate(dateMatcher);
            if (dateStr != null) {
                data.setExtractedDate(dateStr);
            }
        }

        // Extraire le numéro de facture
        Matcher invoiceMatcher = INVOICE_PATTERN.matcher(text);
        if (invoiceMatcher.find()) {
            data.setInvoiceNumber(invoiceMatcher.group(1));
        }

        return data;
    }

    private String extractDate(Matcher matcher) {
        try {
            // Format JJ/MM/AAAA ou JJ-MM-AAAA
            if (matcher.group(1) != null) {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                if (year < 100) year += 2000;
                return String.format("%04d-%02d-%02d", year, month, day);
            }
            // Format JJ mois AAAA
            else if (matcher.group(4) != null) {
                int day = Integer.parseInt(matcher.group(4));
                String monthStr = matcher.group(5).toLowerCase();
                int month = getMonthNumber(monthStr);
                int year = Integer.parseInt(matcher.group(6));
                return String.format("%04d-%02d-%02d", year, month, day);
            }
        } catch (Exception e) {}
        return null;
    }

    private int getMonthNumber(String month) {
        return switch (month.toLowerCase()) {
            case "janvier" -> 1;
            case "fevrier" -> 2;
            case "mars" -> 3;
            case "avril" -> 4;
            case "mai" -> 5;
            case "juin" -> 6;
            case "juillet" -> 7;
            case "aout" -> 8;
            case "septembre" -> 9;
            case "octobre" -> 10;
            case "novembre" -> 11;
            case "decembre" -> 12;
            default -> 1;
        };
    }

    // Classe pour les données extraites
    public static class ExtractedData {
        private Double amount;
        private String transactionId;
        private String errorCode;
        private String email;
        private String extractedDate;
        private String invoiceNumber;
        private List<String> allDetected = new ArrayList<>();

        // Getters et Setters
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getExtractedDate() { return extractedDate; }
        public void setExtractedDate(String extractedDate) { this.extractedDate = extractedDate; }

        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

        public List<String> getAllDetected() { return allDetected; }
        public void setAllDetected(List<String> allDetected) { this.allDetected = allDetected; }

        public boolean hasData() {
            return amount != null || transactionId != null || errorCode != null ||
                    email != null || extractedDate != null || invoiceNumber != null;
        }
    }
}