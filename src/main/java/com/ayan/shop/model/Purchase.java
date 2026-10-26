package com.ayan.shop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    public Customer customer;

    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(precision = 12, scale = 2)
    public BigDecimal total;

    public String status = "AWAITING_PAYMENT";
    public String paymentReference;

    @Column(unique = true)
    public String gatewayOrderId;

    @Column(unique = true)
    public String gatewayPaymentId;

    public String paymentStatus = "PENDING";

    @Column(length = 1000)
    public String deliveryAddress;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<PurchaseItem> items = new ArrayList<>();
}
