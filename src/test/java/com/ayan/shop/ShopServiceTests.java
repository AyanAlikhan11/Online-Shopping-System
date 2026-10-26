package com.ayan.shop;

import com.ayan.shop.model.Customer;
import com.ayan.shop.model.OrderStatusHistory;
import com.ayan.shop.model.Product;
import com.ayan.shop.model.Purchase;
import com.ayan.shop.repository.Customers;
import com.ayan.shop.repository.OrderStatusHistoryRepository;
import com.ayan.shop.repository.Products;
import com.ayan.shop.repository.Purchases;
import com.ayan.shop.service.PaymentService;
import com.ayan.shop.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ShopServiceTests {
    @Autowired
    ShopService service;

    @Autowired
    PaymentService payments;

    @Autowired
    Products products;

    @Autowired
    Customers customers;

    @Autowired
    Purchases purchases;

    @Autowired
    OrderStatusHistoryRepository history;

    @Test
    void checkoutReservesStockAndDemoPaymentAddsHistory() {
        Customer customer = customers.save(new Customer("Buyer One", "buyer-one@example.com", "x", "CUSTOMER"));
        Product product = product("Checkout Item", "100.00", 5);
        Map<Long, Integer> cart = new LinkedHashMap<>();
        cart.put(product.id, 2);

        Purchase order = service.checkout(customer, cart, "12 Test Street");

        assertThat(products.findById(product.id).orElseThrow().stock).isEqualTo(3);
        assertThat(order.total).isEqualByComparingTo("200.00");
        assertThat(order.paymentStatus).isEqualTo("PENDING");

        payments.demoConfirm(order.id, customer);

        assertThat(purchases.findById(order.id).orElseThrow().paymentStatus).isEqualTo("PAID");
        assertThat(history.findByPurchaseIdOrderByCreatedAtAsc(order.id))
                .extracting(event -> event.status)
                .containsExactlyInAnyOrder("AWAITING_PAYMENT", "PLACED");
    }

    @Test
    void cancellingPendingOrderRestoresStock() {
        Customer customer = customers.save(new Customer("Buyer Two", "buyer-two@example.com", "x", "CUSTOMER"));
        Product product = product("Cancel Item", "50.00", 4);
        Map<Long, Integer> cart = new LinkedHashMap<>();
        cart.put(product.id, 3);

        Purchase order = service.checkout(customer, cart, "8 Test Street");
        assertThat(products.findById(product.id).orElseThrow().stock).isEqualTo(1);

        payments.cancel(order.id, customer, products);

        assertThat(products.findById(product.id).orElseThrow().stock).isEqualTo(4);
        assertThat(purchases.findById(order.id).orElseThrow().paymentStatus).isEqualTo("CANCELLED");
        List<OrderStatusHistory> events = history.findByPurchaseIdOrderByCreatedAtAsc(order.id);
        assertThat(events).extracting(event -> event.status)
                .containsExactlyInAnyOrder("AWAITING_PAYMENT", "CANCELLED");
    }

    private Product product(String name, String price, int stock) {
        Product product = new Product();
        product.name = name;
        product.description = "Test product";
        product.category = "Test";
        product.type = "Test";
        product.size = "One Size";
        product.color = "Black";
        product.price = new BigDecimal(price);
        product.stock = stock;
        return products.save(product);
    }
}
