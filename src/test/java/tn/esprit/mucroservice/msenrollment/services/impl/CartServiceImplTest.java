package tn.esprit.mucroservice.msenrollment.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.Cart;
import tn.esprit.mucroservice.msenrollment.entities.CartItem;
import tn.esprit.mucroservice.msenrollment.repositories.CartItemRepository;
import tn.esprit.mucroservice.msenrollment.repositories.CartRepository;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setId(1L);
        cart.setLearnerId("learner1");
        cart.setItems(new ArrayList<>());
    }

    @Test
    void shouldReturnExistingCart() {
        when(cartRepository.findByLearnerId("learner1"))
                .thenReturn(Optional.of(cart));

        Cart result = cartService.getCartByLearnerId("learner1");

        assertNotNull(result);
        assertEquals("learner1", result.getLearnerId());
        verify(cartRepository).findByLearnerId("learner1");
    }

    @Test
    void shouldCreateCartIfNotExists() {
        when(cartRepository.findByLearnerId("newUser"))
                .thenReturn(Optional.empty());

        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.getCartByLearnerId("newUser");

        assertNotNull(result);
        assertEquals("newUser", result.getLearnerId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void shouldAddCourseToCart() {
        when(cartRepository.findByLearnerId("learner1"))
                .thenReturn(Optional.of(cart));

        cartService.addCourseToCart("learner1", 10L, "Java Course", 99.0);

        assertEquals(1, cart.getItems().size());
        assertEquals("Java Course", cart.getItems().get(0).getCourseTitle());

        verify(cartRepository).save(cart);
    }

    @Test
    void shouldNotAddDuplicateCourse() {
        CartItem item = new CartItem();
        item.setCourseId(10L);
        cart.getItems().add(item);

        when(cartRepository.findByLearnerId("learner1"))
                .thenReturn(Optional.of(cart));

        cartService.addCourseToCart("learner1", 10L, "Java Course", 99.0);

        assertEquals(1, cart.getItems().size());

        verify(cartRepository, never()).save(cart);
    }

    @Test
    void shouldRemoveItemFromCart() {
        CartItem item = new CartItem();
        item.setId(1L);
        cart.getItems().add(item);

        when(cartRepository.findByLearnerId("learner1"))
                .thenReturn(Optional.of(cart));

        cartService.removeItemFromCart("learner1", 1L);

        assertTrue(cart.getItems().isEmpty());

        verify(cartRepository).save(cart);
    }

    @Test
    void shouldClearCart() {
        cart.getItems().add(new CartItem());

        when(cartRepository.findByLearnerId("learner1"))
                .thenReturn(Optional.of(cart));

        cartService.clearCart("learner1");

        assertEquals(0, cart.getItems().size());

        verify(cartRepository).save(cart);
    }
}