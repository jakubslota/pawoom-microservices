package com.bookstore.cartservice.controllers;

import com.bookstore.cartservice.dtos.AddToCartRequest;
import com.bookstore.cartservice.entities.CartItem;
import com.bookstore.cartservice.services.CartService;
import com.bookstore.cartservice.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import com.bookstore.cartservice.security.ServiceJwtUtil;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService service;
    private final JwtUtil jwtUtil;
    private final ServiceJwtUtil serviceJwtUtil;

    public CartController(CartService service, JwtUtil jwtUtil, ServiceJwtUtil serviceJwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
        this.serviceJwtUtil = serviceJwtUtil;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader);
        return ResponseEntity.ok(service.getCart(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItem(@RequestBody AddToCartRequest request,
                                     @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader);
        try {
            CartItem item = service.addItem(userId, request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // np. "Not enough quantity"
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.status(404).body("Book not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal error: " + e.getMessage());
        }
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> clearCart(@RequestHeader("Authorization") String authHeader,
                                          @RequestParam(defaultValue = "true") boolean manual) {
        Long userId = jwtUtil.extractUserId(authHeader);
        service.clearCart(userId, manual);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/item/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long productId,
                                           @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader);
        service.removeItem(userId, productId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/internal/{userId}")
    public ResponseEntity<Map<String, Object>> getCartInternal(@PathVariable Long userId,
                                                               @RequestHeader("Authorization") String authHeader) {
        System.out.println("Auth Header: " + authHeader);
        String token = authHeader.replace("Bearer ", "");
        System.out.println("Token: " + token);

        if (!serviceJwtUtil.isValid(token, "order-service")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(service.getCart(userId));
    }
    @DeleteMapping("/internal/{userId}")
    @Transactional
    public ResponseEntity<Void> clearCartInternal(@PathVariable Long userId,
                                                  @RequestParam(defaultValue = "false") boolean manual,
                                                  @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!serviceJwtUtil.isValid(token, "order-service")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.clearCart(userId, manual);
        return ResponseEntity.noContent().build();
    }
}