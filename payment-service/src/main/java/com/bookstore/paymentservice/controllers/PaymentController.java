package com.bookstore.paymentservice.controllers;

import com.bookstore.paymentservice.entities.Payment;
import com.bookstore.paymentservice.repositories.PaymentRepository;
import com.bookstore.paymentservice.services.StripeService;
import com.bookstore.paymentservice.security.ServiceJwtUtil;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.bookstore.paymentservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final StripeService stripeService;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final ServiceJwtUtil serviceJwtUtil;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    public PaymentController(StripeService stripeService,
                             PaymentRepository paymentRepository,
                             RestTemplate restTemplate,
                             JwtUtil jwtUtil,ServiceJwtUtil serviceJwtUtil) {
        this.stripeService = stripeService;
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.serviceJwtUtil = serviceJwtUtil;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, String>> create(@RequestHeader("Authorization") String authHeader) throws Exception {
        Long userId = jwtUtil.extractUserId(authHeader);

        // Przygotuj nagłówki z tokenem JWT
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Pobierz koszyk z CART-SERVICE
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://CART-SERVICE/cart",
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> cart = response.getBody();
        if (cart == null || !cart.containsKey("total")) {
            throw new IllegalStateException("Cart is empty or malformed");
        }

        BigDecimal amount = new BigDecimal(cart.get("total").toString());

        // Utwórz sesję Stripe
        Session session = stripeService.createCheckoutSession(userId, amount, "PLN");
        return ResponseEntity.ok(Map.of("checkoutUrl", session.getUrl()));
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            System.out.println("📩 Webhook received: " + event.getType());

            if ("checkout.session.completed".equals(event.getType())) {
                System.out.println("✅ Session completed event detected");

                Session session = (Session) event.getData().getObject();

                if (session != null) {
                    String sessionId = session.getId();
                    Optional<Payment> optional = paymentRepository.findBySessionId(sessionId);

                    if (optional.isPresent()) {
                        Payment payment = optional.get();
                        payment.setStatus("PAID");
                        paymentRepository.save(payment);
                        String serviceToken = serviceJwtUtil.generateServiceToken("payment-service", 600_000);
                        HttpHeaders headers = new HttpHeaders();
                        System.out.println("📨 Sending request with token: " + serviceToken);
                        headers.set("Authorization",serviceToken);
                        Map<String, Object> body = Map.of(
                                "userId", payment.getUserId(),
                                "status", "READY_TO_SHIP"
                        );
                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                        restTemplate.exchange(
                                orderServiceUrl + "/place/internal",
                                HttpMethod.POST,
                                entity,
                                String.class
                        );

                        return ResponseEntity.ok("Processed");
                    } else {
                        System.out.println("⚠️ No matching payment found for session: " + sessionId);
                    }
                }
            }

        } catch (SignatureVerificationException e) {
            System.out.println("❌ Invalid signature: " + e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (Exception e) {
            System.out.println("❌ Webhook error: " + e.getMessage());
            return ResponseEntity.status(400).body("Error");
        }

        return ResponseEntity.ok("Ignored");
    }
}
