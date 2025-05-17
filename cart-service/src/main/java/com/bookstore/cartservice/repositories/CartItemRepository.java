package com.bookstore.cartservice.repositories;

import com.bookstore.cartservice.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    CartItem findByUserIdAndProductId(Long userId, Long productId);
}
