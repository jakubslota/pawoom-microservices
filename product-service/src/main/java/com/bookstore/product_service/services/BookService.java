package com.bookstore.product_service.services;

import com.bookstore.product_service.entities.Book;
import com.bookstore.product_service.repositories.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book save(Book book) {
        return bookRepository.save(book);
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        bookRepository.deleteById(id);
    }

    public Book update(Long id, Book book) {
        if (!bookRepository.existsById(id)) {
            return null;
        }
        book.setId(id);
        return bookRepository.save(book);
    }
    public boolean decreaseStock(Long id, int qty) {
        Book book = findById(id);
        if (book == null || book.getQuantity() < qty) {
            return false;
        }
        book.setQuantity(book.getQuantity() - qty);
        bookRepository.save(book);
        return true;
    }

    public boolean increaseStock(Long id, int qty) {
        Book book = findById(id);
        if (book == null) {
            return false;
        }
        book.setQuantity(book.getQuantity() + qty);
        bookRepository.save(book);
        return true;
    }

}
