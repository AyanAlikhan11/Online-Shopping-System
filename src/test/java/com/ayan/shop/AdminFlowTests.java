package com.ayan.shop;

import com.ayan.shop.model.Customer;
import com.ayan.shop.model.Product;
import com.ayan.shop.repository.Customers;
import com.ayan.shop.repository.Products;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminFlowTests {
    @Autowired
    MockMvc mvc;

    @Autowired
    Products products;

    @Autowired
    Customers customers;

    @Test
    @WithMockUser(username = "admin@test.local", roles = "ADMIN")
    void adminDashboardRenders() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin dashboard")));
    }

    @Test
    @WithMockUser(username = "admin@test.local", roles = "ADMIN")
    void adminCanCreateProduct() throws Exception {
        mvc.perform(multipart("/admin/products").with(csrf())
                        .param("name", "Admin Added Item")
                        .param("description", "Created from the admin dashboard")
                        .param("category", "Test")
                        .param("type", "Test")
                        .param("size", "One Size")
                        .param("color", "Blue")
                        .param("price", "99.00")
                        .param("stock", "4"))
                .andExpect(status().is3xxRedirection());

        assertThat(products.findAll()).anyMatch(product -> "Admin Added Item".equals(product.name));
    }

    @Test
    @WithMockUser(username = "admin@test.local", roles = "ADMIN")
    void adminCanUploadProductImage() throws Exception {
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Zl1sAAAAASUVORK5CYII=");
        MockMultipartFile image = new MockMultipartFile("imageFiles", "sample.png", "image/png", png);

        mvc.perform(multipart("/admin/products").file(image).with(csrf())
                        .param("name", "Image Upload Item")
                        .param("description", "Product with an uploaded image")
                        .param("price", "10.00")
                        .param("stock", "2"))
                .andExpect(status().is3xxRedirection());

        Product product = products.findAll().stream()
                .filter(item -> "Image Upload Item".equals(item.name))
                .findFirst()
                .orElseThrow();
        assertThat(product.imageUrls).startsWith("/uploads/").endsWith(".png");
        Path stored = Paths.get("target/test-uploads").resolve(product.imageUrls.substring("/uploads/".length()));
        assertThat(Files.exists(stored)).isTrue();
    }

    @Test
    @WithMockUser(username = "shopper@test.local", roles = "CUSTOMER")
    void customerCannotAccessAdminPanel() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    void accessDeniedPageRenders() throws Exception {
        mvc.perform(get("/403"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Access denied")));
    }

    @Test
    @WithMockUser(username = "admin@test.local", roles = "ADMIN")
    void adminCanEditAndDisableCustomer() throws Exception {
        Customer customer = customers.save(new Customer("Old Name", "edit-me@example.com", "x", "CUSTOMER"));

        mvc.perform(post("/admin/customers/" + customer.id).with(csrf())
                        .param("name", "New Name")
                        .param("email", "edit-me@example.com")
                        .param("address", "99 Updated Road")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection());

        Customer updated = customers.findById(customer.id).orElseThrow();
        assertThat(updated.name).isEqualTo("New Name");
        assertThat(updated.address).isEqualTo("99 Updated Road");
        assertThat(updated.active).isTrue();

        mvc.perform(post("/admin/customers/" + customer.id + "/toggle").with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(customers.findById(customer.id).orElseThrow().active).isFalse();
    }
}
