package com.bookstore.orderservice.dtos;

import java.math.BigDecimal;


public class OrderItemDto {
    private String title;
    private String author;
    private int quantity;
    private BigDecimal price;

    public OrderItemDto(String title, String author, int quantity, BigDecimal price) {
        this.title = title;
        this.author = author;
        this.quantity = quantity;
        this.price = price;
    }

    // gettery i settery
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
