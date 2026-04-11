package org.example.msreportingcertification.services.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.example.msreportingcertification.entities.CertificateTemplate;
import org.example.msreportingcertification.entities.EvaluationHistory;
import org.example.msreportingcertification.repositories.CertificateTemplateRepository;
import org.example.msreportingcertification.services.interfaces.ICertificateService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CertificateImpl implements ICertificateService {

    private final CertificateTemplateRepository templateRepository;


    @Override
    public CertificateTemplate saveOrUpdateTemplate(CertificateTemplate template) {
        if (template.getEvaluationId() != null) {
            return templateRepository.findByEvaluationId(template.getEvaluationId())
                    .map(existing -> {

                        existing.setHtmlContent(template.getHtmlContent());
                        existing.setPlatformLogoBase64(template.getPlatformLogoBase64());
                        existing.setTrainerSignatureBase64(template.getTrainerSignatureBase64());
                        existing.setTrainerName(template.getTrainerName());

                        existing.setTemplateDefault(false);
                        System.out.println("updated");
                        return templateRepository.save(existing);
                    })
                    .orElseGet(() -> {
                        template.setTemplateDefault(false);
                        return templateRepository.save(template);
                    });
        }
        else {

            return templateRepository.findDefaultTemplate()
                    .map(existing -> {
                        existing.setHtmlContent(template.getHtmlContent());
                        existing.setPlatformLogoBase64(template.getPlatformLogoBase64());
                        existing.setTrainerSignatureBase64(template.getTrainerSignatureBase64());
                        existing.setTrainerName(template.getTrainerName());

                        return templateRepository.save(existing);
                    })
                    .orElseGet(() -> {
                        template.setTemplateDefault(true);
                        return templateRepository.save(template);
                    });
        }
    }

    // --- PARTIE 2 : GÉNÉRATION DU PDF (Pour l'Apprenant) ---

    @Override
    public byte[] generateCertificatePdf(EvaluationHistory history, CertificateTemplate template) throws Exception {
        String myIp = getRealIpAddress();
        String myPort = "8081"; // Ton port backend
        String verificationUrl = "http://" + myIp + ":" + myPort + "/verify/" + history.getId();
        String qrBase64 = generateQRCodeBase64(verificationUrl);

        String htmlFinal = template.getHtmlContent()
                .replace("[[LEARNER_NAME]]", escapeHtml(history.getLearnerName()))
                .replace("[[EVALUATION_TITLE]]", escapeHtml(history.getEvaluationTitle()))
                .replace("[[SCORE]]", history.getPercentage() + "%")
                .replace("[[DATE]]", formatDate(history.getReceivedAt()))
                .replace("[[QR_CODE]]", qrBase64)
                .replace("[[ID]]", history.getId().toString());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlFinal, "/");
            builder.useDefaultPageSize(297, 210, PdfRendererBuilder.PageSizeUnits.MM);
            builder.useFastMode();
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        return date.format(formatter);
    }

    private String generateQRCodeBase64(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200, hints);

        try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        }
    }

    private String getRealIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                java.util.Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "localhost";
    }


}