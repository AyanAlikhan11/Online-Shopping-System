package com.ayan.shop.controller;

import com.ayan.shop.model.Customer;
import com.ayan.shop.model.OrderStatusHistory;
import com.ayan.shop.model.Product;
import com.ayan.shop.model.Purchase;
import com.ayan.shop.repository.Customers;
import com.ayan.shop.repository.Favorites;
import com.ayan.shop.repository.OrderStatusHistoryRepository;
import com.ayan.shop.repository.Products;
import com.ayan.shop.repository.Purchases;
import com.ayan.shop.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final List<String> DELIVERY_STATUSES =
            List.of("PLACED", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED");

    private final Products products;
    private final Customers customers;
    private final Purchases orders;
    private final Favorites favorites;
    private final OrderStatusHistoryRepository history;
    private final FileStorageService storage;

    public AdminController(Products products, Customers customers, Purchases orders, Favorites favorites,
                           OrderStatusHistoryRepository history, FileStorageService storage) {
        this.products = products;
        this.customers = customers;
        this.orders = orders;
        this.favorites = favorites;
        this.history = history;
        this.storage = storage;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("products", products.findAll());
        model.addAttribute("customers", customers.findAll());
        model.addAttribute("orders", orders.findAll());
        return "admin";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        model.addAttribute("product", new Product());
        return "product-form";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        model.addAttribute("product", products.findById(id).orElseThrow());
        return "product-form";
    }

    @PostMapping("/products")
    public String saveProduct(@RequestParam(required = false) Long id,
                              @RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) String type,
                              @RequestParam(required = false) String size,
                              @RequestParam(required = false) String color,
                              @RequestParam BigDecimal price,
                              @RequestParam int stock,
                              @RequestParam(required = false) String imageUrls,
                              @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {
        Product p = id == null ? new Product() : products.findById(id).orElseThrow();
        p.name = name.trim();
        p.description = description == null ? null : description.trim();
        p.category = category == null ? null : category.trim();
        p.type = type == null ? null : type.trim();
        p.size = size == null ? null : size.trim();
        p.color = color == null ? null : color.trim();
        p.price = price;
        p.stock = stock;

        List<String> storedUrls = storage.store(imageFiles);
        String combinedUrls = imageUrls == null ? "" : imageUrls.trim();
        if (!storedUrls.isEmpty()) {
            String uploadedJoined = String.join("\n", storedUrls);
            combinedUrls = combinedUrls.isBlank() ? uploadedJoined : combinedUrls + "\n" + uploadedJoined;
        }
        p.imageUrls = combinedUrls.isBlank() ? null : combinedUrls;

        products.save(p);
        return "redirect:/admin";
    }

    @PostMapping("/products/{id}/delete")
    @Transactional
    public String deleteProduct(@PathVariable Long id) {
        favorites.deleteByProductId(id);
        products.deleteById(id);
        return "redirect:/admin";
    }

    @PostMapping("/orders/{id}/status")
    @Transactional
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) String note) {
        Purchase purchase = orders.locked(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        String normalized = status.toUpperCase(Locale.ROOT).trim();
        if (!DELIVERY_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }

        if (normalized.equals(purchase.status)) {
            return "redirect:/orders/" + id;
        }

        purchase.status = normalized;
        orders.save(purchase);

        String recordNote = (note == null || note.isBlank())
                ? "Status updated to " + normalized + " by admin"
                : note.trim();
        history.save(new OrderStatusHistory(purchase, normalized, recordNote));
        return "redirect:/orders/" + id;
    }

    @PostMapping("/customers/{id}/toggle-status")
    public String toggleCustomerStatus(@PathVariable Long id) {
        Customer customer = customers.findById(id).orElseThrow();
        customer.active = customer.active == null || !customer.active;
        customers.save(customer);
        return "redirect:/admin";
    }

    @PostMapping("/customers/{id}/delete")
    @Transactional
    public String deleteCustomer(@PathVariable Long id) {
        Customer customer = customers.findById(id).orElseThrow();
        if ("ADMIN".equalsIgnoreCase(customer.role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete an administrator account");
        }
        favorites.deleteByCustomerId(id);
        customers.delete(customer);
        return "redirect:/admin";
    }
}
