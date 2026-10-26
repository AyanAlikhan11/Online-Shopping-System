package com.ayan.shop.config;

import com.ayan.shop.model.Customer;
import com.ayan.shop.model.Product;
import com.ayan.shop.repository.Customers;
import com.ayan.shop.repository.Products;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seed(Customers users, Products products, PasswordEncoder encoder,
                           @Value("${app.admin.email}") String email,
                           @Value("${app.admin.password}") String password) {
        return args -> {
            if (users.findByEmailIgnoreCase(email).isEmpty()) {
                users.save(new Customer("Administrator", email, encoder.encode(password), "ADMIN"));
            }

            if (products.count() == 0) {
                String[][] sample = {
                        {"Wireless Headphones", "Electronics", "Audio", "One Size", "Black", "2499", "12",
                                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=900,https://images.unsplash.com/photo-1484704849700-f032a568e944?w=900"},
                        {"Classic Sneakers", "Fashion", "Shoes", "42", "White", "3299", "8",
                                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=900,https://images.unsplash.com/photo-1549298916-b41d501d3772?w=900"},
                        {"Minimal Watch", "Accessories", "Watch", "One Size", "Brown", "1899", "15",
                                "https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=900"}
                };

                for (String[] row : sample) {
                    Product p = new Product();
                    p.name = row[0];
                    p.description = "Explore " + row[0] + " with a clean design and reliable everyday performance.";
                    p.category = row[1];
                    p.type = row[2];
                    p.size = row[3];
                    p.color = row[4];
                    p.price = new BigDecimal(row[5]);
                    p.stock = Integer.parseInt(row[6]);
                    p.imageUrls = row[7];
                    products.save(p);
                }
            }
        };
    }
}
