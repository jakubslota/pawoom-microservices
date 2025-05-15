package com.bookstore.orderservice.controllers;

import com.bookstore.orderservice.entities.Order;
import com.bookstore.orderservice.services.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/place/{userId}")
    public ResponseEntity<Order> placeOrder(@PathVariable Long userId,
                                            @RequestParam(defaultValue = "CREATED") String status) {
        Order order = service.placeOrder(userId, status);
        return ResponseEntity.ok(order);
    }
}
