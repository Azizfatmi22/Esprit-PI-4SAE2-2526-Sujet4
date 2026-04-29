package tn.esprit.mucroservice.msenrollment.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.Invoice;
import tn.esprit.mucroservice.msenrollment.entities.InvoiceStatus;
import tn.esprit.mucroservice.msenrollment.repositories.InvoiceRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceServiceImpl service;

    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoice = Invoice.builder()
                .id(1L)
                .learnerId("learner1")
                .paymentId(100L)
                .totalAmount(200.0)
                .currency("TND")
                .status(InvoiceStatus.PAID)
                .build();
    }

    // ======================
    // GENERATE INVOICE (4 params)
    // ======================
    @Test
    void shouldGenerateInvoiceBasic() {

        when(invoiceRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        Invoice result = service.generateInvoice(
                "learner1",
                100L,
                200.0,
                List.of("Java")
        );

        assertNotNull(result);
        assertEquals("learner1", result.getLearnerId());
        assertNotNull(result.getInvoiceNumber());
    }

    // ======================
    // GET BY ID
    // ======================
    @Test
    void shouldReturnInvoiceById() {

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(invoice));

        Invoice result = service.getInvoiceById(1L);

        assertEquals(1L, result.getId());
    }

    // ======================
    // EXCEPTION NOT FOUND
    // ======================
    @Test
    void shouldThrowWhenInvoiceNotFound() {

        when(invoiceRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.getInvoiceById(99L));

        assertTrue(ex.getMessage().contains("non trouvée"));
    }

    // ======================
    // GET ALL
    // ======================
    @Test
    void shouldReturnAllInvoices() {

        when(invoiceRepository.findAll())
                .thenReturn(List.of(invoice, invoice));

        List<Invoice> result = service.getAllInvoices();

        assertEquals(2, result.size());
    }

    // ======================
    // GET BY LEARNER
    // ======================
    @Test
    void shouldReturnInvoicesByLearner() {

        when(invoiceRepository.findByLearnerId("learner1"))
                .thenReturn(List.of(invoice));

        List<Invoice> result =
                service.getInvoicesByLearner("learner1");

        assertEquals(1, result.size());
    }

    // ======================
    // DELETE
    // ======================
    @Test
    void shouldDeleteInvoice() {

        service.deleteInvoice(1L);

        verify(invoiceRepository).deleteById(1L);
    }

    // ======================
    // DIRECT INVOICES FILTER
    // ======================
    @Test
    void shouldReturnDirectInvoices() {

        invoice.setInstallmentPlanId(null);

        when(invoiceRepository.findAll())
                .thenReturn(List.of(invoice));

        List<Invoice> result = service.getDirectInvoices();

        assertEquals(1, result.size());
    }

    // ======================
    // INSTALLMENT INVOICES FILTER
    // ======================
    @Test
    void shouldReturnInstallmentInvoices() {

        invoice.setInstallmentPlanId(10L);

        when(invoiceRepository.findAll())
                .thenReturn(List.of(invoice));

        List<Invoice> result = service.getInstallmentInvoices();

        assertEquals(1, result.size());
    }
}