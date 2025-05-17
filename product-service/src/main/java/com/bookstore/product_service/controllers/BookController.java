package com.bookstore.product_service.controllers;

import com.bookstore.product_service.entities.Book;
import com.bookstore.product_service.services.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<Book> save(@RequestBody Book book) {
        return ResponseEntity.ok(bookService.save(book));
    }

    @GetMapping
    public ResponseEntity<List<Book>> findAll() {
        return ResponseEntity.ok(bookService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> findById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> update(@PathVariable("id") Long id, @RequestBody Book book) {
        return ResponseEntity.ok(bookService.update(id, book));
    }

    @PutMapping("/{id}/decrease")
    public ResponseEntity<?> decreaseQuantity(@PathVariable Long id, @RequestParam int qty) {
        boolean success = bookService.decreaseStock(id, qty);
        if (!success) return ResponseEntity.badRequest().body("Not enough quantity or book not found");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/increase")
    public ResponseEntity<?> increaseQuantity(@PathVariable Long id, @RequestParam int qty) {
        boolean success = bookService.increaseStock(id, qty);
        if (!success) return ResponseEntity.badRequest().body("Book not found");
        return ResponseEntity.ok().build();
    }
}
