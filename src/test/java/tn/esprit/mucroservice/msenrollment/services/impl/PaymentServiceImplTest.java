package tn.esprit.mucroservice.msenrollment.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.PaymentRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.ICartService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IEnrollmentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ICartService cartService;

    @Mock
    private IEnrollmentService enrollmentService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setLearnerId("learner1");

        CartItem item = new CartItem();
        item.setCourseId(1L);
        item.setCourseTitle("Java");
        item.setCoursePrice(100.0);

        List<CartItem> items = new ArrayList<>();
        items.add(item);

        cart.setItems(items);
    }

    // ======================
    // GET ALL PAYMENTS
    // ======================
    @Test
    void shouldReturnAllPayments() {
        when(paymentRepository.findAll())
                .thenReturn(List.of(new Payment(), new Payment()));

        List<Payment> result = paymentService.getAllPayments();

        assertEquals(2, result.size());
    }

    // ======================
    // GET BY LEARNER
    // ======================
    @Test
    void shouldReturnPaymentsByLearner() {
        when(paymentRepository.findByLearnerId("learner1"))
                .thenReturn(List.of(new Payment()));

        List<Payment> result =
                paymentService.getPaymentsByLearner("learner1");

        assertEquals(1, result.size());
    }

    // ======================
    // SUCCESS PAYMENT
    // ======================
    @Test
    void shouldProcessPaymentSuccessfully() {

        when(cartService.getCartByLearnerId("learner1"))
                .thenReturn(cart);

        when(paymentRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.processPayment(
                "learner1",
                100.0,
                PaymentMethod.FLOUCI
        );

        assertNotNull(result);
        assertEquals("learner1", result.getLearnerId());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertNotNull(result.getTransactionId());

        verify(paymentRepository).save(any());
    }

    // ======================
    // CART NULL
    // ======================
    @Test
    void shouldThrowWhenCartIsNull() {

        when(cartService.getCartByLearnerId("learner1"))
                .thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.processPayment(
                        "learner1",
                        100.0,
                        PaymentMethod.FLOUCI
                )
        );

        assertTrue(ex.getMessage().contains("Le panier est null"));
    }

    // ======================
    // ITEMS NULL
    // ======================
    @Test
    void shouldThrowWhenCartItemsNull() {

        cart.setItems(null);

        when(cartService.getCartByLearnerId("learner1"))
                .thenReturn(cart);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.processPayment(
                        "learner1",
                        100.0,
                        PaymentMethod.FLOUCI
                )
        );

        assertTrue(ex.getMessage().contains("items du panier"));
    }

    // ======================
    // CART EMPTY
    // ======================
    @Test
    void shouldThrowWhenCartEmpty() {

        cart.setItems(new ArrayList<>());

        when(cartService.getCartByLearnerId("learner1"))
                .thenReturn(cart);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.processPayment(
                        "learner1",
                        100.0,
                        PaymentMethod.FLOUCI
                )
        );

        assertTrue(ex.getMessage().contains("panier est vide"));
    }

    // ======================
    // DELETE PAYMENT
    // ======================
    @Test
    void shouldDeletePayment() {
        paymentService.deletePayment(1L);

        verify(paymentRepository).deleteById(1L);
    }
}