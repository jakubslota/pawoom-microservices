package com.bookstore.cartservice.services;

import com.bookstore.cartservice.entities.CartItem;
import com.bookstore.cartservice.repositories.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;

    public CartService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
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

    public CartItem addItem(CartItem item) {
        return cartItemRepository.save(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
