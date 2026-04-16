package com.example.mstrainerhiring.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentClient {

    private final RestTemplate restTemplate;

    @Value("${services.enrollment.url:http://localhost:8089}")
    private String enrollmentUrl;

    public void enrollLearner(String learnerId, Long courseId, String courseTitle, Double coursePrice) {
        log.info("Enrolling learner {} in course {} via MS-Enrollment", learnerId, courseId);

        // 1. Add to cart
        String addUrl = enrollmentUrl + "/msenrollment/cart/" + learnerId + "/add";
        Map<String, Object> addBody = new HashMap<>();
        addBody.put("courseId", courseId);
        addBody.put("courseTitle", courseTitle);
        addBody.put("coursePrice", coursePrice);

        try {
            restTemplate.postForEntity(addUrl, addBody, Object.class);
            log.info("Course added to cart for learner {}", learnerId);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 409) {
                log.info("Course already in cart for learner {}, proceeding to confirmation", learnerId);
            } else {
                log.warn("Failed to add course to cart: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Error adding course to cart: {}", e.getMessage());
        }

        // 2. Confirm payment (0.0 amount for coupon)
        try {
            String confirmUrl = enrollmentUrl + "/msenrollment/payment/confirm/" + learnerId;
            Map<String, Object> confirmBody = new HashMap<>();
            confirmBody.put("amount", 0.0);
            confirmBody.put("method", "BAKCHICH");
            confirmBody.put("couponCode", null);

            restTemplate.postForEntity(confirmUrl, confirmBody, Object.class);
            log.info("Enrollment confirmed for learner {} in course {}", learnerId, courseId);

        } catch (Exception e) {
            log.error("Failed to enroll learner {} in course {}: {}", learnerId, courseId, e.getMessage());
        }
    }
}
