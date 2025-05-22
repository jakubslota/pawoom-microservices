package com.bookstore.orderservice.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {
    private Long id;
    private LocalDateTime orderDate;
    private String status;
    private List<OrderItemDto> items;

    public OrderDto() {}

    public OrderDto(Long id, LocalDateTime orderDate, String status, List<OrderItemDto> items) {
        this.id = id;
        this.orderDate = orderDate;
        this.status = status;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
