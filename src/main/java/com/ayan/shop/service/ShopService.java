package com.ayan.shop.service;

import com.ayan.shop.model.Customer;
import com.ayan.shop.model.OrderStatusHistory;
import com.ayan.shop.model.Product;
import com.ayan.shop.model.Purchase;
import com.ayan.shop.model.PurchaseItem;
import com.ayan.shop.repository.OrderStatusHistoryRepository;
import com.ayan.shop.repository.Products;
import com.ayan.shop.repository.Purchases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ShopService {
    private static final Logger log = LoggerFactory.getLogger(ShopService.class);

    private final Products products;
    private final Purchases purchases;
    private final JavaMailSender mail;
    private final OrderStatusHistoryRepository history;

    @Value("${app.mail.from}")
    String from;

    public ShopService(Products products, Purchases purchases, JavaMailSender mail, OrderStatusHistoryRepository history) {
        this.products = products;
        this.purchases = purchases;
        this.mail = mail;
        this.history = history;
    }

    @Transactional
    public Purchase checkout(Customer customer, Map<Long, Integer> cart, String address) {
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Enter a delivery address");
        }

        Purchase order = new Purchase();
        order.customer = customer;
        order.deliveryAddress = address.trim();
        order.total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Product product = products.locked(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Product unavailable"));
            int quantity = entry.getValue();
            if (quantity < 1 || product.stock < quantity) {
                throw new IllegalArgumentException("Insufficient stock for " + product.name);
            }
            product.stock -= quantity;

            PurchaseItem item = new PurchaseItem();
            item.purchase = order;
            item.productId = product.id;
            item.productName = product.name;
            item.unitPrice = product.price;
            item.quantity = quantity;
            order.items.add(item);
            order.total = order.total.add(product.price.multiply(BigDecimal.valueOf(quantity)));
        }

        Purchase saved = purchases.save(order);
        history.save(new OrderStatusHistory(saved, saved.status, "Order created awaiting payment"));
        return saved;
    }

    @Async("mailExecutor")
    public void sendOrderConfirmation(Purchase purchase) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(purchase.customer.email);
            message.setSubject("Order #" + purchase.id + " Confirmed");
            message.setText("Thank you for your order!\nTotal: INR " + purchase.total + "\nStatus: " + purchase.status);
            mail.send(message);
        } catch (Exception e) {
            log.warn("Failed to send order email: {}", e.getMessage());
        }
    }
}
