package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.entities.Cart;

public interface ICartService {

    Cart getCartByLearnerId(String learnerId);

    void addCourseToCart(String learnerId, Long courseId, String courseTitle, Double coursePrice);

    void removeItemFromCart(String learnerId, Long itemId);

    void clearCart(String learnerId);

   // double getTotalPrice(String learnerId);
}