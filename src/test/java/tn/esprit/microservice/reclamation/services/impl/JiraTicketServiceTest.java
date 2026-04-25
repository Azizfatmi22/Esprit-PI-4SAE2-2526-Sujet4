package tn.esprit.microservice.reclamation.services.impl;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationType;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JiraTicketServiceTest {

    private JiraTicketService service;

    @BeforeEach
    void setUp() {
        service = new JiraTicketService();

        // Injection manuelle des @Value
        ReflectionTestUtils.setField(service, "jiraUrl", "https://test.atlassian.net");
        ReflectionTestUtils.setField(service, "jiraEmail", "admin@test.com");
        ReflectionTestUtils.setField(service, "jiraToken", "123456");
        ReflectionTestUtils.setField(service, "projectKey", "SUP");
        ReflectionTestUtils.setField(service, "issueType", "Task");
    }

    private Reclamation buildReclamation() {
        Reclamation r = new Reclamation();
        r.setId(1L);
        r.setLearnerId("UUID-123");
        r.setSubject("Erreur paiement");
        r.setDescription("Impossible de payer");
        r.setType(ReclamationType.PAYMENT);
        r.setPriority(1);
        r.setCreatedDate(LocalDateTime.now());
        r.setAmount(150.0);
        r.setTransactionId("TX999");
        r.setCourseId(10L);
        return r;
    }

    @Test
    void testMapPriority() throws Exception {

        Method method = JiraTicketService.class
                .getDeclaredMethod("mapPriority", Integer.class);

        method.setAccessible(true);

        assertEquals("Highest", method.invoke(service, 1));
        assertEquals("High", method.invoke(service, 2));
        assertEquals("Medium", method.invoke(service, 3));
        assertEquals("Low", method.invoke(service, 5));
        assertEquals("Medium", method.invoke(service, (Object) null));
    }

    @Test
    void testMapPriorityLabel() throws Exception {

        Method method = JiraTicketService.class
                .getDeclaredMethod("mapPriorityLabel", Integer.class);

        method.setAccessible(true);

        assertEquals("Haute 🔴", method.invoke(service, 1));
        assertEquals("Moyenne 🟡", method.invoke(service, 2));
        assertEquals("Basse 🟢", method.invoke(service, 3));
        assertEquals("Non définie", method.invoke(service, 5));
        assertEquals("Moyenne", method.invoke(service, (Object) null));
    }

    @Test
    void testTruncate() throws Exception {

        Method method = JiraTicketService.class
                .getDeclaredMethod("truncate", String.class, int.class);

        method.setAccessible(true);

        String shortText = "Hello";
        String longText = "abcdefghijklmnopqrstuvwxyz";

        assertEquals("Hello", method.invoke(service, shortText, 10));
        assertEquals("abcdefg...", method.invoke(service, longText, 10));
        assertEquals("", method.invoke(service, null, 10));
    }

    @Test
    void testBuildPlainDescription() throws Exception {

        Reclamation r = buildReclamation();

        Method method = JiraTicketService.class
                .getDeclaredMethod("buildPlainDescription", Reclamation.class);

        method.setAccessible(true);

        String result = (String) method.invoke(service, r);

        assertTrue(result.contains("Réclamation #1"));
        assertTrue(result.contains("UUID-123"));
        assertTrue(result.contains("Erreur paiement"));
        assertTrue(result.contains("Impossible de payer"));
    }

    @Test
    void testBuildAdditionalInfo() throws Exception {

        Reclamation r = buildReclamation();

        Method method = JiraTicketService.class
                .getDeclaredMethod("buildAdditionalInfo", Reclamation.class);

        method.setAccessible(true);

        String result = (String) method.invoke(service, r);

        assertTrue(result.contains("Cours concerné"));
        assertTrue(result.contains("Transaction"));
        assertTrue(result.contains("Montant"));
    }

    @Test
    void testBuildJiraPayload() throws Exception {

        Reclamation r = buildReclamation();

        Method method = JiraTicketService.class
                .getDeclaredMethod("buildJiraPayload", Reclamation.class);

        method.setAccessible(true);

        Map<String, Object> payload =
                (Map<String, Object>) method.invoke(service, r);

        assertNotNull(payload);
        assertTrue(payload.containsKey("fields"));
    }
}