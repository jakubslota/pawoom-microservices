package com.bookstore.cartservice.services;

import com.bookstore.cartservice.entities.CartItem;
import com.bookstore.cartservice.repositories.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final RestTemplate restTemplate;

    public CartService(CartItemRepository cartItemRepository, RestTemplate restTemplate) {
        this.cartItemRepository = cartItemRepository;
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        BigDecimal total = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        return result;
    }

    public CartItem addItem(Long userId, Long productId, int quantity) {
            restTemplate.put(
                    "http://PRODUCT-SERVICE/products/" + productId + "/decrease?qty=" + quantity,
                    null
            );

            Map book = restTemplate.getForObject(
                    "http://PRODUCT-SERVICE/products/" + productId,
                    Map.class
            );
            BigDecimal price = new BigDecimal(book.get("price").toString());

            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setPrice(price);

            return cartItemRepository.save(item);
        }


    public void clearCart(Long userId, boolean manual) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);

        if (manual) {
            for (CartItem item : items) {
                restTemplate.put(
                        "http://PRODUCT-SERVICE/products/" + item.getProductId() + "/increase?qty=" + item.getQuantity(),
                        null
                );
            }
        }

        cartItemRepository.deleteByUserId(userId);
    }

    public void removeItem(Long userId, Long productId) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId);
        if (item != null) {
            restTemplate.put(
                    "http://PRODUCT-SERVICE/products/" + productId + "/increase?qty=" + item.getQuantity(),
                    null
            );
            cartItemRepository.delete(item);
        }
    }
}