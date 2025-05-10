package com.bookstore.cartorderservice.services;

import com.bookstore.cartorderservice.dtos.ProductDto;
import com.bookstore.cartorderservice.entities.CartItem;
import com.bookstore.cartorderservice.entities.Order;
import com.bookstore.cartorderservice.entities.OrderItem;
import com.bookstore.cartorderservice.repositories.CartItemRepository;
import com.bookstore.cartorderservice.repositories.OrderItemRepository;
import com.bookstore.cartorderservice.repositories.OrderRepository;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final CartItemRepository cartRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final RestTemplate restTemplate;

    public OrderService(CartItemRepository cartRepo,
                        OrderRepository orderRepo,
                        OrderItemRepository orderItemRepo,
                        RestTemplateBuilder builder) {
        this.cartRepo = cartRepo;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.restTemplate = builder.build();
    }

    public Order placeOrder(Long userId) {
        List<CartItem> cart = cartRepo.findByUserId(userId);
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cart is empty. Cannot place order.");
        }

        Order order = orderRepo.save(new Order(userId, LocalDateTime.now(), "CREATED"));

        for (CartItem item : cart) {
            BigDecimal price = getProductPrice(item.getProductId());
            OrderItem orderItem = new OrderItem(order.getId(), item.getProductId(), item.getQuantity(), price);
            orderItemRepo.save(orderItem);
        }

        cartRepo.deleteByUserId(userId);
        return order;
    }

    private BigDecimal getProductPrice(Long productId) {
        String url = "http://PRODUCT-SERVICE/products/" + productId;
        ResponseEntity<ProductDto> response = restTemplate.getForEntity(url, ProductDto.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getPrice();
        } else {
            throw new IllegalStateException("Failed to fetch product price for productId=" + productId);
        }
    }
}
