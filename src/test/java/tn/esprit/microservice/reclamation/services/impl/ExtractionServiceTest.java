package tn.esprit.microservice.reclamation.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.microservice.reclamation.services.impl.ExtractionService.ExtractedData;

import static org.junit.jupiter.api.Assertions.*;

class ExtractionServiceTest {

    private ExtractionService service;

    @BeforeEach
    void setUp() {
        service = new ExtractionService();
    }

    @Test
    void testExtractAllData() {

        String text = """
            Paiement de 150.50 DT échoué.
            transaction TXN-ABC123
            erreur ERR-404
            email test@mail.com
            date 15/04/2025
            facture INV-99999
            """;

        ExtractedData data = service.extract(text);

        assertEquals(150.50, data.getAmount());
        assertEquals("TXN-ABC123", data.getTransactionId());
        assertEquals("ERR-404", data.getErrorCode());
        assertEquals("test@mail.com", data.getEmail());
        assertEquals("2025-04-15", data.getExtractedDate());

        // correction selon résultat réel regex
        assertEquals("INV-99999", data.getInvoiceNumber());

        assertTrue(data.hasData());
    }
    @Test
    void testExtractFrenchDate() {

        String text = "incident déclaré le 12 avril 2025";

        ExtractedData data = service.extract(text);

        assertEquals("2025-04-12", data.getExtractedDate());
    }

    @Test
    void testExtractEmptyText() {

        ExtractedData data = service.extract("");

        assertFalse(data.hasData());
        assertNull(data.getAmount());
    }

    @Test
    void testExtractNullText() {

        ExtractedData data = service.extract(null);

        assertFalse(data.hasData());
    }

    @Test
    void testExtractNoMatch() {

        ExtractedData data =
                service.extract("bonjour je veux une information");

        assertFalse(data.hasData());
    }
}
