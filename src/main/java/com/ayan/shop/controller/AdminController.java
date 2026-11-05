package com.ayan.shop.controller;

import com.ayan.shop.model.*;
import com.ayan.shop.repository.*;
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
import java.util.Optional;

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
        model.addAttribute("customers", customers.findAll().stream()
                .filter(c -> !"ADMIN".equalsIgnoreCase(c.role)).toList());
        model.addAttribute("orders", orders.findAll());
        model.addAttribute("deliveryStatuses", DELIVERY_STATUSES);
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
        Product product = id == null ? new Product() : products.findById(id).orElseThrow();
        product.name = name.trim();
        product.description = trim(description);
        product.category = trim(category);
        product.type = trim(type);
        product.size = trim(size);
        product.color = trim(color);
        product.price = price;
        product.stock = stock;

        List<String> stored = storage.store(imageFiles);
        String urls = imageUrls == null ? "" : imageUrls.trim();
        if (!stored.isEmpty()) {
            String uploaded = String.join("\n", stored);
            urls = urls.isBlank() ? uploaded : urls + "\n" + uploaded;
        }
        product.imageUrls = urls.isBlank() ? null : urls;

        products.save(product);
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
        String text = note == null || note.isBlank()
                ? "Status updated to " + normalized + " by admin"
                : note.trim();
        history.save(new OrderStatusHistory(purchase, normalized, text));
        return "redirect:/orders/" + id;
    }

    @GetMapping("/customers/{id}/edit")
    public String editCustomer(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customers.findById(id).orElseThrow());
        return "customer-form";
    }

    @PostMapping("/customers/{id}")
    public String saveCustomer(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam String email,
                               @RequestParam(required = false) String address,
                               @RequestParam(required = false) Boolean active,
                               Model model) {
        Customer customer = customers.findById(id).orElseThrow();
        Optional<Customer> duplicate = customers.findByEmailIgnoreCase(email);
        if (duplicate.isPresent() && !duplicate.get().id.equals(id)) {
            model.addAttribute("customer", customer);
            model.addAttribute("error", "Email already in use");
            return "customer-form";
        }
        customer.name = name.trim();
        customer.email = email.trim().toLowerCase(Locale.ROOT);
        customer.address = trim(address);
        customer.active = active != null && active;
        customers.save(customer);
        return "redirect:/admin";
    }

    @PostMapping("/customers/{id}/toggle")
    public String toggleCustomer(@PathVariable Long id) {
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
        if (orders.existsByCustomerId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delete is only allowed for customers without orders");
        }
        favorites.deleteByCustomerId(id);
        customers.delete(customer);
        return "redirect:/admin";
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
