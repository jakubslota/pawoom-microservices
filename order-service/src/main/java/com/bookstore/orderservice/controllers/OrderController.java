package com.bookstore.orderservice.controllers;

import com.bookstore.orderservice.dtos.OrderDto;
import com.bookstore.orderservice.entities.Order;
import com.bookstore.orderservice.services.OrderService;
import com.bookstore.orderservice.security.ServiceJwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;
    private final ServiceJwtUtil serviceJwtUtil;

    public OrderController(OrderService service, ServiceJwtUtil serviceJwtUtil) {
        this.service = service;
        this.serviceJwtUtil = serviceJwtUtil;
    }

    // Składanie zamówienia przez użytkownika (z tokenem użytkownika)
//    @PostMapping("/place")
//    public ResponseEntity<Order> placeOrder(
//            @RequestHeader("Authorization") String authHeader,
//            @RequestParam(defaultValue = "CREATED") String status
//    ) {
//        Order order = service.placeOrder(authHeader, status);
//        return ResponseEntity.ok(order);
//    }

    // Składanie zamówienia przez zaufany serwis (np. payment-service)
    @PostMapping("/place/internal")
    public ResponseEntity<Order> placeOrderInternal(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        System.out.println("Received internal order request with body: " + body);
        System.out.println("Authorization header: " + authHeader);

        if (!serviceJwtUtil.isValid(authHeader, "payment-service")) {
            System.out.println("Unauthorized service request.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = Long.valueOf(body.get("userId").toString());
        String status = body.getOrDefault("status", "CREATED").toString();
        Order order = service.placeOrderInternal(userId, status);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getMyOrders(
            @RequestHeader("Authorization") String authHeader
    ) {
        List<OrderDto> orders = service.getMyOrders(authHeader);
        return ResponseEntity.ok(orders);
    }
}