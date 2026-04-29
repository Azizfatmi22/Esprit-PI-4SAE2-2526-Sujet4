package tn.esprit.mucroservice.msenrollment.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.WafaAccount;
import tn.esprit.mucroservice.msenrollment.repositories.WafaAccountRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WafaServiceImplTest {

    @Mock
    private WafaAccountRepository wafaRepo;

    @InjectMocks
    private WafaServiceImpl service;

    private WafaAccount account;

    @BeforeEach
    void setUp() {
        account = new WafaAccount();
        account.setId(1L);
        account.setPhoneNumber("12345678");
        account.setLearnerId("learner1");
        account.setBalance(500.0);
    }

    @Test
    void shouldReturnBalance() {
        when(wafaRepo.findByPhoneNumber("12345678"))
                .thenReturn(Optional.of(account));

        Double result = service.getBalance("12345678");

        assertEquals(500.0, result);
    }

    @Test
    void shouldThrowIfAccountNotFound() {
        when(wafaRepo.findByPhoneNumber("99999999"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.getBalance("99999999"));

        assertTrue(ex.getMessage().contains("Aucun compte Wafa Cash"));
    }

    @Test
    void shouldReturnTrueIfSufficientBalance() {
        when(wafaRepo.findByPhoneNumber("12345678"))
                .thenReturn(Optional.of(account));

        assertTrue(service.hasSufficientBalance("12345678", 200.0));
    }

    @Test
    void shouldReturnFalseIfInsufficientBalance() {
        when(wafaRepo.findByPhoneNumber("12345678"))
                .thenReturn(Optional.of(account));

        assertFalse(service.hasSufficientBalance("12345678", 800.0));
    }

    @Test
    void shouldReturnFalseIfAccountNotFound() {
        when(wafaRepo.findByPhoneNumber("12345678"))
                .thenReturn(Optional.empty());

        assertFalse(service.hasSufficientBalance("12345678", 100.0));
    }

    @Test
    void shouldDebitAccount() {
        when(wafaRepo.findByPhoneNumber("12345678"))
                .thenReturn(Optional.of(account));

        when(wafaRepo.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        WafaAccount result =
                service.debitAccount("12345678", 100.0);

        assertEquals(400.0, result.getBalance());
    }

    @Test
    void shouldThrowIfInsufficientFundsOnDebit() {
        when(wafaRepo.findByPhoneNumber("12345678"))
                .thenReturn(Optional.of(account));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.debitAccount("12345678", 1000.0));

        assertTrue(ex.getMessage().contains("Solde insuffisant"));
    }

    @Test
    void shouldCreditAccount() {
        when(wafaRepo.findByPhoneNumber("12345678"))
                .thenReturn(Optional.of(account));

        when(wafaRepo.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        WafaAccount result =
                service.creditAccount("12345678", 150.0);

        assertEquals(650.0, result.getBalance());
    }

    @Test
    void shouldReturnExistingAccount() {
        when(wafaRepo.findByPhoneNumber("12345678"))
                .thenReturn(Optional.of(account));

        WafaAccount result =
                service.getOrCreateAccount("12345678", "learner1");

        assertEquals("12345678", result.getPhoneNumber());
    }

    @Test
    void shouldCreateNewAccount() {
        when(wafaRepo.findByPhoneNumber("55555555"))
                .thenReturn(Optional.empty());

        when(wafaRepo.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        WafaAccount result =
                service.getOrCreateAccount("55555555", "newLearner");

        assertEquals("55555555", result.getPhoneNumber());
        assertEquals("newLearner", result.getLearnerId());
        assertTrue(result.getBalance() >= 100.0);
    }
}