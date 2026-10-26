package com.ayan.shop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_items")
public class PurchaseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    public Purchase purchase;

    public Long productId;
    public String productName;

    @Column(precision = 12, scale = 2)
    public BigDecimal unitPrice;

    public int quantity;
}
