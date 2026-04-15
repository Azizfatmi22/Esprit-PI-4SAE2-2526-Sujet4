package com.example.msforum.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${app.mail.from}")
  private String fromAddress;

  @Value("${app.api-url}")
  private String apiUrl;

  /**
   * Sends an HTML confirmation email with a clickable confirmation link.
   * Runs asynchronously so it never blocks the HTTP response.
   */
  @Async
  public void sendParticipationConfirmation(String toEmail,
      String participantName,
      String eventName,
      long eventId,
      String token) {
    String confirmUrl = apiUrl + "/events/" + eventId + "/participants/confirm?token=" + token;

    String html = buildEmailHtml(participantName, eventName, confirmUrl);

    try {
      MimeMessage msg = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(msg, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(toEmail);
      helper.setSubject("✅ Confirm your participation in \"" + eventName + "\"");
      helper.setText(html, true); // true = HTML
      mailSender.send(msg);
      log.info("[Email] Confirmation sent to {} for event '{}'", toEmail, eventName);
    } catch (MessagingException e) {
      log.error("[Email] Failed to send confirmation to {}: {}", toEmail, e.getMessage());
    }
  }

  private String buildEmailHtml(String name, String eventName, String confirmUrl) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
          <title>Confirm Participation</title>
        </head>
        <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 0;">
            <tr>
              <td align="center">
                <table width="600" cellpadding="0" cellspacing="0"
                       style="background:#ffffff;border-radius:16px;overflow:hidden;
                              box-shadow:0 10px 40px rgba(0,0,0,0.12);">
                  <!-- Header -->
                  <tr>
                    <td style="background:linear-gradient(135deg,#00f2fe 0%%,#4facfe 100%%);
                               padding:40px 48px;text-align:center;">
                      <h1 style="margin:0;color:#ffffff;font-size:28px;font-weight:800;
                                 letter-spacing:-0.5px;">🎉 Almost There!</h1>
                      <p style="margin:10px 0 0;color:rgba(255,255,255,0.9);font-size:16px;">
                        Confirm your spot for <strong>%s</strong>
                      </p>
                    </td>
                  </tr>
                  <!-- Body -->
                  <tr>
                    <td style="padding:40px 48px;">
                      <p style="margin:0 0 16px;font-size:16px;color:#334155;">
                        Hi <strong>%s</strong> 👋
                      </p>
                      <p style="margin:0 0 24px;font-size:15px;color:#64748b;line-height:1.6;">
                        You requested to participate in <strong>%s</strong>.
                        Click the button below to confirm your spot. The link expires after first use.
                      </p>
                      <!-- CTA Button -->
                      <table cellpadding="0" cellspacing="0" width="100%%">
                        <tr>
                          <td align="center">
                            <a href="%s"
                               style="display:inline-block;padding:16px 40px;
                                      background:linear-gradient(135deg,#00f2fe 0%%,#4facfe 100%%);
                                      color:#ffffff;text-decoration:none;
                                      border-radius:50px;font-size:16px;font-weight:700;
                                      box-shadow:0 8px 24px rgba(0,162,254,0.35);
                                      letter-spacing:0.3px;">
                              ✅ Confirm My Participation
                            </a>
                          </td>
                        </tr>
                      </table>
                      <p style="margin:28px 0 0;font-size:13px;color:#94a3b8;text-align:center;">
                        Or copy this link into your browser:<br/>
                        <a href="%s" style="color:#4facfe;word-break:break-all;">%s</a>
                      </p>
                    </td>
                  </tr>
                  <!-- Footer -->
                  <tr>
                    <td style="background:#f8fafc;padding:24px 48px;text-align:center;
                               border-top:1px solid #e2e8f0;">
                      <p style="margin:0;font-size:12px;color:#94a3b8;">
                        If you didn't request this, you can safely ignore this email.
                      </p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(eventName, name, eventName, confirmUrl, confirmUrl, confirmUrl);
  }
}
