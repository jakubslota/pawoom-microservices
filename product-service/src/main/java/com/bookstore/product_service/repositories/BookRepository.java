package com.bookstore.product_service.repositories;

import com.bookstore.product_service.entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
