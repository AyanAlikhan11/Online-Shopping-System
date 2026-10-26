package com.ayan.shop;

import com.ayan.shop.model.Customer;
import com.ayan.shop.model.Product;
import com.ayan.shop.model.Purchase;
import com.ayan.shop.repository.Customers;
import com.ayan.shop.repository.Products;
import com.ayan.shop.service.PaymentService;
import com.ayan.shop.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShopApplicationTests {
    @Autowired
    MockMvc mvc;

    @Autowired
    Customers customers;

    @Autowired
    Products products;

    @Autowired
    ShopService service;

    @Autowired
    PaymentService payments;

    @Test
    void publicPagesAndHealthEndpointLoad() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void adminPageRequiresAuthentication() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    void missingProductReturnsCustomNotFoundPage() throws Exception {
        mvc.perform(get("/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found")));
    }

    @Test
    @WithMockUser(username = "order-page@example.com", roles = "CUSTOMER")
    void orderPageRendersTrackingHistory() throws Exception {
        Customer customer = customers.save(new Customer("Order Page", "order-page@example.com", "x", "CUSTOMER"));
        Product product = new Product();
        product.name = "Tracking Item";
        product.price = new BigDecimal("25.00");
        product.stock = 2;
        products.save(product);
        Map<Long, Integer> cart = new LinkedHashMap<>();
        cart.put(product.id, 1);
        Purchase order = service.checkout(customer, cart, "10 Tracking Road");
        payments.demoConfirm(order.id, customer);

        mvc.perform(get("/orders/" + order.id))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Delivery tracking")))
                .andExpect(content().string(containsString("PLACED")));
    }

    @Test
    void registrationCreatesCustomer() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("name", "Test Buyer")
                        .param("email", "test-buyer@example.com")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection());

        assertThat(customers.findByEmailIgnoreCase("test-buyer@example.com")).isPresent();
    }
}
