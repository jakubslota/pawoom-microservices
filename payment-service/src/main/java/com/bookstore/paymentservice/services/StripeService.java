package com.bookstore.paymentservice.services;

import com.bookstore.paymentservice.entities.Payment;
import com.bookstore.paymentservice.repositories.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class StripeService {

    private final PaymentRepository paymentRepository;

    @Value("${stripe.secret.key}")
    private String secretKey;

    public StripeService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Session createCheckoutSession(Long userId, BigDecimal amount, String currency) throws Exception {
        Stripe.apiKey = secretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://example.com/success")
                .setCancelUrl("https://example.com/cancel")
                .addExpand("payment_intent")  // 🔧 KLUCZOWE DLA WEBHOOKA
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency(currency)
                                        .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Stripe używa groszy
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName("Koszyk zamówienia")
                                                        .build())
                                        .build())
                        .build())
                .putMetadata("userId", userId.toString())
                .build();

        Session session = Session.create(params);

        paymentRepository.save(new Payment(
                userId,
                session.getId(),
                amount,
                currency,
                "PENDING",
                LocalDateTime.now()
        ));

        return session;
    }
}