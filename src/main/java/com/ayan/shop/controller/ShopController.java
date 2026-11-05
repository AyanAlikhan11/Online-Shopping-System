package com.ayan.shop.controller;

import com.ayan.shop.model.*;
import com.ayan.shop.repository.*;
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
import java.util.Comparator;
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
    public String index(Model model,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String type,
                        @RequestParam(required = false) String size,
                        @RequestParam(required = false) String color,
                        @RequestParam(required = false) BigDecimal min,
                        @RequestParam(required = false) BigDecimal max) {
        List<Product> all = products.findAll();
        String term = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        List<Product> list = all.stream()
                .filter(p -> term == null || contains(p.name, term) || contains(p.description, term) || contains(p.category, term))
                .filter(p -> same(category, p.category))
                .filter(p -> same(type, p.type))
                .filter(p -> same(size, p.size))
                .filter(p -> same(color, p.color))
                .filter(p -> min == null || p.price.compareTo(min) >= 0)
                .filter(p -> max == null || p.price.compareTo(max) <= 0)
                .sorted(Comparator.comparing(p -> p.name.toLowerCase(Locale.ROOT)))
                .toList();

        model.addAttribute("products", list);
        model.addAttribute("categories", distinct(all, p -> p.category));
        model.addAttribute("types", distinct(all, p -> p.type));
        model.addAttribute("sizes", distinct(all, p -> p.size));
        model.addAttribute("colors", distinct(all, p -> p.color));
        return "home";
    }

    @GetMapping("/products/{id}")
    public String product(@PathVariable Long id, Model model) {
        model.addAttribute("product", products.findById(id).orElseThrow());
        return "product";
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        List<CartRow> rows = cartRows(session);
        model.addAttribute("rows", rows);
        model.addAttribute("total", rows.stream().map(CartRow::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("livePayment", payments.live());
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int qty,
                            HttpSession session,
                            @RequestHeader(value = "Referer", required = false) String referer) {
        Product product = products.findById(productId).orElse(null);
        if (product == null || qty < 1) {
            return "redirect:/";
        }
        Map<Long, Integer> cart = cart(session);
        if (cart.getOrDefault(productId, 0) + qty > product.stock) {
            return "redirect:/products/" + productId + "?stock";
        }
        cart.merge(productId, qty, Integer::sum);
        session.setAttribute("cart", cart);
        return "redirect:" + (referer == null ? "/cart" : referer);
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long productId, @RequestParam int qty, HttpSession session) {
        Map<Long, Integer> cart = cart(session);
        if (qty < 1) {
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

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam String address, HttpSession session, Authentication auth) {
        Purchase purchase = service.checkout(currentUser(auth), cart(session), address);
        session.removeAttribute("cart");
        return "redirect:/pay/" + purchase.id;
    }

    @GetMapping("/pay/{id}")
    public String payPage(@PathVariable Long id, Model model, Authentication auth) {
        assertOwnerOrAdmin(purchases.findById(id).orElseThrow(), auth);
        model.addAttribute("order", payments.prepareOrder(id));
        model.addAttribute("livePayment", payments.live());
        model.addAttribute("keyId", payments.getKeyId());
        return "pay";
    }

    @PostMapping("/pay/{id}/demo")
    public String demoPay(@PathVariable Long id, Authentication auth) {
        Purchase purchase = purchases.findById(id).orElseThrow();
        assertOwnerOrAdmin(purchase, auth);
        service.sendOrderConfirmation(payments.demoConfirm(id, purchase.customer));
        return "redirect:/orders/" + id;
    }

    @PostMapping("/pay/{id}/verify")
    public String verifyPayment(@PathVariable Long id,
                                @RequestParam(required = false) String razorpay_payment_id,
                                @RequestParam(required = false) String razorpay_order_id,
                                @RequestParam(required = false) String razorpay_signature,
                                Authentication auth) {
        Purchase purchase = purchases.findById(id).orElseThrow();
        assertOwnerOrAdmin(purchase, auth);
        service.sendOrderConfirmation(
                payments.verifyPayment(id, razorpay_payment_id, razorpay_order_id, razorpay_signature));
        return "redirect:/orders/" + id;
    }

    @PostMapping("/pay/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Authentication auth) {
        Purchase purchase = purchases.findById(id).orElseThrow();
        assertOwnerOrAdmin(purchase, auth);
        payments.cancel(id, purchase.customer, products);
        return "redirect:/orders/" + id;
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model, Authentication auth) {
        Purchase purchase = purchases.findById(id).orElseThrow();
        assertOwnerOrAdmin(purchase, auth);
        model.addAttribute("order", purchase);
        model.addAttribute("history", history.findByPurchaseIdOrderByCreatedAtAsc(id));
        return "order";
    }

    @GetMapping("/orders")
    public String myOrders(Model model, Authentication auth) {
        model.addAttribute("orders", purchases.findByCustomerIdOrderByCreatedAtDesc(currentUser(auth).id));
        return "orders";
    }

    @GetMapping("/favorites")
    public String myFavorites(Model model, Authentication auth) {
        model.addAttribute("favorites", favorites.findByCustomerId(currentUser(auth).id));
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
                    Favorite favorite = new Favorite();
                    favorite.customer = customer;
                    favorite.product = products.findById(productId).orElseThrow();
                    favorites.save(favorite);
                });
        return "redirect:" + (referer == null ? "/favorites" : referer);
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
        if (password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters");
            return "register";
        }
        customers.save(new Customer(name.trim(), email.trim().toLowerCase(Locale.ROOT),
                encoder.encode(password), "CUSTOMER"));
        return "redirect:/login?registered";
    }

    @GetMapping("/403")
    public String forbidden(Model model) {
        return WebAdvice.page(model, 403, "Access denied",
                "Your account does not have permission to open this page.");
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

    private List<CartRow> cartRows(HttpSession session) {
        return cart(session).entrySet().stream()
                .map(e -> products.findById(e.getKey())
                        .map(p -> new CartRow(p, e.getValue(), p.price.multiply(BigDecimal.valueOf(e.getValue()))))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
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
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !purchase.customer.id.equals(user.id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        }
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private static boolean same(String filter, String value) {
        return filter == null || filter.isBlank() || (value != null && value.equalsIgnoreCase(filter));
    }

    private static List<String> distinct(List<Product> list, Function<Product, String> field) {
        return list.stream().map(field).filter(v -> v != null && !v.isBlank()).distinct().sorted().toList();
    }

    public record CartRow(Product product, int quantity, BigDecimal subtotal) {
    }
}
