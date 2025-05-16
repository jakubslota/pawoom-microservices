package com.bookstore.paymentservice.controllers;

import com.bookstore.paymentservice.entities.Payment;
import com.bookstore.paymentservice.repositories.PaymentRepository;
import com.bookstore.paymentservice.services.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.model.EventDataObjectDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    public PaymentController(StripeService stripeService, PaymentRepository paymentRepository, RestTemplate restTemplate) {
        this.stripeService = stripeService;
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, String>> create(@RequestBody Map<String, Object> request) throws Exception {
        Long userId = Long.valueOf(request.get("userId").toString());
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String currency = request.get("currency").toString();
        Session session = stripeService.createCheckoutSession(userId, amount, currency);
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
                    System.out.println("🔍 Stripe Session ID: " + session.getId());

                    String sessionId = session.getId();
                    Optional<Payment> optional = paymentRepository.findBySessionId(sessionId);

                    if (optional.isPresent()) {
                        System.out.println("💾 Found payment in DB for session: " + sessionId);
                        Payment payment = optional.get();
                        payment.setStatus("PAID");
                        paymentRepository.save(payment);

                        Long userId = payment.getUserId();
                        restTemplate.postForObject(orderServiceUrl + userId + "?status=READY_TO_SHIP", null, String.class);

                        return ResponseEntity.ok("Processed");
                    } else {
                        System.out.println("⚠️ No matching payment found for session: " + sessionId);
                    }
                } else {
                    System.out.println("⚠️ Session is null in webhook event");
                }
            } else {
                System.out.println("ℹ️ Ignoring non-session-completed event.");
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

