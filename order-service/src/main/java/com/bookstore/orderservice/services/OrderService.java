package com.bookstore.orderservice.services;

import com.bookstore.orderservice.dtos.OrderDto;
import com.bookstore.orderservice.dtos.OrderItemDto;
import com.bookstore.orderservice.entities.Order;
import com.bookstore.orderservice.entities.OrderItem;
import com.bookstore.orderservice.repositories.OrderItemRepository;
import com.bookstore.orderservice.repositories.OrderRepository;
import com.bookstore.orderservice.util.JwtUtil;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.bookstore.orderservice.security.ServiceJwtUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final ServiceJwtUtil serviceJwtUtil;

    public OrderService(OrderRepository orderRepo,
                        OrderItemRepository orderItemRepo,
                        RestTemplate restTemplate,
                        JwtUtil jwtUtil, ServiceJwtUtil serviceJwtUtil) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.serviceJwtUtil = serviceJwtUtil;
    }

    public Order placeOrderInternal(Long userId, String status) {
        // Pobierz koszyk użytkownika – z tokenem serwisowym
        String serviceToken = serviceJwtUtil.generateServiceToken("order-service", 600_000);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://CART-SERVICE/cart/internal/" + userId,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> cartResponse = response.getBody();
        if (cartResponse == null || !cartResponse.containsKey("items")) {
            throw new IllegalStateException("Cart is empty or unavailable");
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) cartResponse.get("items");
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Order order = new Order(userId, LocalDateTime.now(), status);
        order = orderRepo.save(order);

        for (Map<String, Object> item : items) {
            Long productId = Long.valueOf(item.get("productId").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());
            BigDecimal price = new BigDecimal(item.get("price").toString());
            orderItemRepo.save(new OrderItem(order.getId(), productId, quantity, price));
        }

        // Wyczyść koszyk
        restTemplate.exchange(
                "http://CART-SERVICE/cart/internal/" + userId + "?manual=false",
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        return order;
    }

    public List<OrderDto> getMyOrders(String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader);
        List<Order> orders = orderRepo.findByUserId(userId);

        return orders.stream().map(order -> {
            List<OrderItemDto> items = order.getItems().stream().map(item -> {
                String title = "Unknown";
                String author = "Unknown";
                try {
                    ResponseEntity<Map> response = restTemplate.getForEntity(
                            "http://PRODUCT-SERVICE/products/" + item.getProductId(),
                            Map.class
                    );
                    Map body = response.getBody();
                    if (body != null) {
                        title = body.get("title").toString();
                        author = body.get("author").toString();
                    }
                } catch (Exception e) {
                    System.out.println("Error fetching book info: " + e.getMessage());
                }

                return new OrderItemDto(title, author, item.getQuantity(), item.getPrice());
            }).toList();

            return new OrderDto(order.getId(), order.getOrderDate(), order.getStatus(), items);
        }).toList();
    }
}
