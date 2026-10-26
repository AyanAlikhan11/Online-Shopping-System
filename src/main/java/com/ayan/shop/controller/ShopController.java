package com.ayan.shop.controller;

import com.ayan.shop.model.Customer;
import com.ayan.shop.model.Favorite;
import com.ayan.shop.model.Product;
import com.ayan.shop.model.Purchase;
import com.ayan.shop.repository.Customers;
import com.ayan.shop.repository.Favorites;
import com.ayan.shop.repository.OrderStatusHistoryRepository;
import com.ayan.shop.repository.Products;
import com.ayan.shop.repository.Purchases;
import com.ayan.shop.service.PaymentService;
import com.ayan.shop.service.ShopService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Controller
public class ShopController {
    private final Customers customers;
    private final Products products;
    private final Purchases purchases;
    private final Favorites favorites;
    private final ShopService service;
    private final PasswordEncoder encoder;
    private final PaymentService payments;
    private final OrderStatusHistoryRepository history;

    public ShopController(Customers customers, Products products, Purchases purchases, Favorites favorites,
                          ShopService service, PasswordEncoder encoder, PaymentService payments,
                          OrderStatusHistoryRepository history) {
        this.customers = customers;
        this.products = products;
        this.purchases = purchases;
        this.favorites = favorites;
        this.service = service;
        this.encoder = encoder;
        this.payments = payments;
        this.history = history;
    }

    @GetMapping("/")
    public String index(Model model, Authentication auth,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String sort) {
        List<Product> list = products.findAll();

        if (search != null && !search.isBlank()) {
            String query = search.trim().toLowerCase(Locale.ROOT);
            list = list.stream()
                    .filter(p -> p.name.toLowerCase(Locale.ROOT).contains(query)
                            || (p.description != null && p.description.toLowerCase(Locale.ROOT).contains(query))
                            || (p.category != null && p.category.toLowerCase(Locale.ROOT).contains(query)))
                    .toList();
        }

        if (category != null && !category.isBlank()) {
            list = list.stream()
                    .filter(p -> p.category != null && p.category.equalsIgnoreCase(category))
                    .toList();
        }

        if ("price-asc".equalsIgnoreCase(sort)) {
            list = list.stream().sorted((a, b) -> a.price.compareTo(b.price)).toList();
        } else if ("price-desc".equalsIgnoreCase(sort)) {
            list = list.stream().sorted((a, b) -> b.price.compareTo(a.price)).toList();
        } else if ("name-desc".equalsIgnoreCase(sort)) {
            list = list.stream().sorted((a, b) -> b.name.compareToIgnoreCase(a.name)).toList();
        } else {
            list = list.stream().sorted((a, b) -> a.name.compareToIgnoreCase(b.name)).toList();
        }

        model.addAttribute("products", list);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("sort", sort);

        Customer current = currentUser(auth);
        List<Long> favoriteIds = current == null ? List.of()
                : favorites.findByCustomerId(current.id).stream().map(f -> f.product.id).toList();
        model.addAttribute("favoriteIds", favoriteIds);
        return "home";
    }

    @GetMapping("/products/{id}")
    public String product(@PathVariable Long id, Model model, Authentication auth) {
        Product p = products.findById(id).orElseThrow();
        Customer current = currentUser(auth);
        boolean isFav = current != null && favorites.findByCustomerIdAndProductId(current.id, id).isPresent();
        model.addAttribute("product", p);
        model.addAttribute("isFavorite", isFav);
        return "product";
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Map<Long, Integer> cart = cart(session);
        List<CartRow> rows = cart.entrySet().stream()
                .map(e -> products.findById(e.getKey())
                        .map(p -> new CartRow(p, e.getValue(), p.price.multiply(BigDecimal.valueOf(e.getValue()))))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        BigDecimal total = rows.stream().map(CartRow::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("rows", rows);
        model.addAttribute("total", total);
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int qty,
                            HttpSession session,
                            @RequestHeader(value = "Referer", required = false) String referer) {
        if (qty > 0) {
            Map<Long, Integer> cart = cart(session);
            cart.merge(productId, qty, Integer::sum);
            session.setAttribute("cart", cart);
        }
        return "redirect:" + (referer != null ? referer : "/cart");
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long productId, @RequestParam int qty, HttpSession session) {
        Map<Long, Integer> cart = cart(session);
        if (qty <= 0) {
            cart.remove(productId);
        } else {
            cart.put(productId, qty);
        }
        session.setAttribute("cart", cart);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId, HttpSession session) {
        Map<Long, Integer> cart = cart(session);
        cart.remove(productId);
        session.setAttribute("cart", cart);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model, Authentication auth) {
        Customer customer = currentUser(auth);
        Map<Long, Integer> cart = cart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        List<CartRow> rows = cart.entrySet().stream()
                .map(e -> products.findById(e.getKey())
                        .map(p -> new CartRow(p, e.getValue(), p.price.multiply(BigDecimal.valueOf(e.getValue()))))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();

        BigDecimal total = rows.stream().map(CartRow::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("rows", rows);
        model.addAttribute("total", total);
        model.addAttribute("customer", customer);
        return "customer-form";
    }

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam String address, HttpSession session, Authentication auth) {
        Customer customer = currentUser(auth);
        Map<Long, Integer> cart = cart(session);
        Purchase purchase = service.checkout(customer, cart, address);
        session.removeAttribute("cart");
        return "redirect:/orders/" + purchase.id + "/pay";
    }

    @GetMapping("/orders/{id}/pay")
    public String payPage(@PathVariable Long id, Model model, Authentication auth) {
        Purchase purchase = purchases.findById(id).orElseThrow();
        assertOwnerOrAdmin(purchase, auth);
        Purchase prepared = payments.prepareOrder(id);

        model.addAttribute("order", prepared);
        model.addAttribute("live", payments.live());
        model.addAttribute("razorpayKey", payments.getKeyId());
        model.addAttribute("amountInPaise", prepared.total.movePointRight(2).longValueExact());
        return "pay";
    }

    @PostMapping("/orders/{id}/pay/verify")
    public String verifyPayment(@PathVariable Long id,
                                @RequestParam(required = false) String razorpay_payment_id,
                                @RequestParam(required = false) String razorpay_order_id,
                                @RequestParam(required = false) String razorpay_signature,
                                Authentication auth) {
        Purchase purchase = purchases.findById(id).orElseThrow();
        assertOwnerOrAdmin(purchase, auth);
        payments.verifyPayment(id, razorpay_payment_id, razorpay_order_id, razorpay_signature);
        service.sendOrderConfirmation(purchase);
        return "redirect:/orders/" + id;
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model, Authentication auth) {
        Purchase purchase = purchases.findById(id).orElseThrow();
        assertOwnerOrAdmin(purchase, auth);
        model.addAttribute("order", purchase);
        model.addAttribute("timeline", history.findByPurchaseIdOrderByCreatedAtAsc(id));
        return "order";
    }

    @GetMapping("/orders")
    public String myOrders(Model model, Authentication auth) {
        Customer customer = currentUser(auth);
        model.addAttribute("orders", purchases.findByCustomerIdOrderByCreatedAtDesc(customer.id));
        return "orders";
    }

    @GetMapping("/favorites")
    public String myFavorites(Model model, Authentication auth) {
        Customer customer = currentUser(auth);
        model.addAttribute("favorites", favorites.findByCustomerId(customer.id));
        return "favorites";
    }

    @PostMapping("/favorites/toggle")
    public String toggleFavorite(@RequestParam Long productId,
                                 Authentication auth,
                                 @RequestHeader(value = "Referer", required = false) String referer) {
        Customer customer = currentUser(auth);
        favorites.findByCustomerIdAndProductId(customer.id, productId).ifPresentOrElse(
                favorites::delete,
                () -> {
                    Product p = products.findById(productId).orElseThrow();
                    Favorite fav = new Favorite();
                    fav.customer = customer;
                    fav.product = p;
                    favorites.save(fav);
                }
        );
        return "redirect:" + (referer != null ? referer : "/favorites");
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam String password,
                                 Model model) {
        if (customers.findByEmailIgnoreCase(email).isPresent()) {
            model.addAttribute("error", "Email already in use");
            return "register";
        }
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters");
            return "register";
        }
        customers.save(new Customer(name.trim(), email.trim().toLowerCase(Locale.ROOT), encoder.encode(password), "CUSTOMER"));
        return "redirect:/login?registered";
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> cart(HttpSession session) {
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    private Customer currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return customers.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }

    private void assertOwnerOrAdmin(Purchase purchase, Authentication auth) {
        Customer user = currentUser(auth);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !purchase.customer.id.equals(user.id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        }
    }

    public record CartRow(Product product, int quantity, BigDecimal subtotal) {}
}
