package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;   // ← IMPORT CORRIGÉ (Spring)

import tn.esprit.mucroservice.msenrollment.entities.Cart;
import tn.esprit.mucroservice.msenrollment.entities.CartItem;
import tn.esprit.mucroservice.msenrollment.repositories.CartRepository;
import tn.esprit.mucroservice.msenrollment.repositories.CartItemRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.ICartService;

import java.util.ArrayList;

@Service
public class CartServiceImpl implements ICartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = false)   // ← EXPLICITEMENT en écriture
    public Cart getCartByLearnerId(String learnerId) {
        // ✅ Gérer l'Optional
        Cart cart = cartRepository.findByLearnerId(learnerId).orElse(null);

        if (cart == null) {
            cart = new Cart();
            cart.setLearnerId(learnerId);
            cart.setItems(new ArrayList<>());
            cart = cartRepository.save(cart);
        }
        return cart;
    }

    @Override
    @Transactional
    public void addCourseToCart(String learnerId, Long courseId, String courseTitle, Double coursePrice) {
        Cart cart = getCartByLearnerId(learnerId);

        // ✅ Si déjà dans le panier, on ignore silencieusement
        boolean alreadyInCart = cart.getItems().stream()
                .anyMatch(item -> item.getCourseId().equals(courseId));

        if (alreadyInCart) {
            return; // ✅ Ne rien faire au lieu de lancer une exception
        }

        CartItem item = new CartItem();
        item.setCourseId(courseId);
        item.setCourseTitle(courseTitle);
        item.setCoursePrice(coursePrice);
        item.setCart(cart);
        cart.getItems().add(item);

        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void removeItemFromCart(String learnerId, Long itemId) {
        Cart cart = getCartByLearnerId(learnerId);
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(String learnerId) {
        Cart cart = getCartByLearnerId(learnerId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public double getTotalPrice(String learnerId) {
//        Cart cart = getCartByLearnerId(learnerId);
//        return cart.getTotalPrice();
//    }
}