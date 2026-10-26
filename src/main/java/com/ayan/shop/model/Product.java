package com.ayan.shop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Arrays;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(length = 3000)
    public String description;

    public String category;
    public String type;
    public String size;
    public String color;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal price = BigDecimal.ZERO;

    public int stock;

    @Column(length = 4000)
    public String imageUrls;

    public String[] images() {
        return imageUrls == null ? new String[0] : Arrays.stream(imageUrls.split("\\s*,\\s*|\\R")).filter(x -> !x.isBlank()).toArray(String[]::new);
    }

    public String firstImage() {
        return images().length > 0 ? images()[0] : "/img/product-placeholder.svg";
    }
}
