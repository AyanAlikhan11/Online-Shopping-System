package com.ayan.shop.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class ProductionConfig {
    @Value("${app.admin.password}")
    String adminPassword;

    @Value("${app.razorpay.key-id:}")
    String keyId;

    @Value("${app.razorpay.key-secret:}")
    String secret;

    @PostConstruct
    void validate() {
        if ("ChangeMe123!".equals(adminPassword) || adminPassword.length() < 12) {
            throw new IllegalStateException("Set a unique ADMIN_PASSWORD of at least 12 characters before production startup");
        }
        if (keyId.isBlank() || secret.isBlank()) {
            throw new IllegalStateException("Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET before production startup");
        }
    }
}
