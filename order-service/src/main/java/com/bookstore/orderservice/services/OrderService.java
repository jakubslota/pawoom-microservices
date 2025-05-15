package com.bookstore.orderservice.services;

import com.bookstore.orderservice.entities.Order;
import com.bookstore.orderservice.entities.OrderItem;
import com.bookstore.orderservice.repositories.OrderItemRepository;
import com.bookstore.orderservice.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final RestTemplate restTemplate;

    public OrderService(OrderRepository orderRepo, OrderItemRepository orderItemRepo, RestTemplate restTemplate) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.restTemplate = restTemplate;
    }

    public Order placeOrder(Long userId, String status) {
        Map<String, Object> cartResponse = restTemplate.getForObject(
                "http://CART-SERVICE/cart/" + userId,
                Map.class
        );

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

        restTemplate.delete("http://CART-SERVICE/cart/" + userId);
        return order;
    }
}
