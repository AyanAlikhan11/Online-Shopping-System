package com.ayan.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    public Purchase purchase;

    @Column(nullable = false)
    public String status;

    @Column(length = 500)
    public String note;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public OrderStatusHistory() {
    }

    public OrderStatusHistory(Purchase purchase, String status, String note) {
        this.purchase = purchase;
        this.status = status;
        this.note = note;
    }
}
