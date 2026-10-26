package com.ayan.shop.service;

import com.ayan.shop.model.Customer;
import com.ayan.shop.model.OrderStatusHistory;
import com.ayan.shop.model.Purchase;
import com.ayan.shop.repository.OrderStatusHistoryRepository;
import com.ayan.shop.repository.Purchases;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {
    private final Purchases purchases;
    private final OrderStatusHistoryRepository history;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    @Value("${app.payment.mode:demo}")
    String mode;
    @Value("${app.razorpay.key-id:}")
    String keyId;
    @Value("${app.razorpay.key-secret:}")
    String secret;

    public PaymentService(Purchases purchases, ObjectMapper mapper, OrderStatusHistoryRepository history) {
        this.purchases = purchases;
        this.mapper = mapper;
        this.history = history;
    }

    public boolean live() {
        return "razorpay".equalsIgnoreCase(mode) && !keyId.isBlank() && !secret.isBlank();
    }

    public String getKeyId() {
        return keyId;
    }

    @Transactional
    public Purchase prepareOrder(Long id) {
        Purchase purchase = purchases.locked(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!"AWAITING_PAYMENT".equalsIgnoreCase(purchase.status)) {
            return purchase;
        }

        if (!live()) {
            if (purchase.gatewayOrderId == null || purchase.gatewayOrderId.isBlank()) {
                purchase.gatewayOrderId = "order_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            purchase.paymentStatus = "CREATED";
            return purchases.save(purchase);
        }

        if (purchase.gatewayOrderId != null && !purchase.gatewayOrderId.isBlank()) {
            return purchase;
        }

        try {
            long amountInPaise = purchase.total.movePointRight(2).longValueExact();
            String payload = mapper.writeValueAsString(Map.of(
                    "amount", amountInPaise,
                    "currency", "INR",
                    "receipt", "order_rcptid_" + purchase.id,
                    "notes", Map.of("orderId", String.valueOf(purchase.id))
            ));

            String credentials = Base64.getEncoder().encodeToString((keyId + ":" + secret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay error: " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            purchase.gatewayOrderId = root.path("id").asText();
            purchase.paymentStatus = "CREATED";
            return purchases.save(purchase);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to connect to Razorpay: " + e.getMessage());
        }
    }

    @Transactional
    public Purchase verifyPayment(Long id, String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
        Purchase purchase = purchases.locked(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if ("CONFIRMED".equalsIgnoreCase(purchase.status)) {
            return purchase;
        }

        if (!live()) {
            purchase.paymentReference = "SIM-" + UUID.randomUUID().toString().substring(0, 8);
            purchase.gatewayPaymentId = purchase.paymentReference;
            purchase.paymentStatus = "PAID";
            purchase.status = "PLACED";
            history.save(new OrderStatusHistory(purchase, "PLACED", "Demo payment simulated successfully"));
            return purchases.save(purchase);
        }

        if (razorpayPaymentId == null || razorpayPaymentId.isBlank()
                || razorpayOrderId == null || razorpayOrderId.isBlank()
                || razorpaySignature == null || razorpaySignature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incomplete payment callback");
        }

        if (!razorpayOrderId.equals(purchase.gatewayOrderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order ID mismatch");
        }

        try {
            String data = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            if (!MessageDigest.isEqual(hex.toString().getBytes(StandardCharsets.UTF_8),
                    razorpaySignature.getBytes(StandardCharsets.UTF_8))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment signature");
            }

            purchase.gatewayPaymentId = razorpayPaymentId;
            purchase.paymentReference = razorpayPaymentId;
            purchase.paymentStatus = "PAID";
            purchase.status = "PLACED";
            history.save(new OrderStatusHistory(purchase, "PLACED", "Razorpay payment verified: " + razorpayPaymentId));
            return purchases.save(purchase);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Verification failed: " + e.getMessage());
        }
    }

    @Transactional
    public Purchase demoConfirm(Long id, Customer customer) {
        Purchase purchase = purchases.locked(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (customer != null && !purchase.customer.id.equals(customer.id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        }
        purchase.paymentReference = "SIM-" + UUID.randomUUID().toString().substring(0, 8);
        purchase.gatewayPaymentId = purchase.paymentReference;
        purchase.paymentStatus = "PAID";
        purchase.status = "PLACED";
        history.save(new OrderStatusHistory(purchase, "PLACED", "Demo payment confirmed"));
        return purchases.save(purchase);
    }

    @Transactional
    public Purchase cancel(Long id, Customer customer, com.ayan.shop.repository.Products productsRepo) {
        Purchase purchase = purchases.locked(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (customer != null && !purchase.customer.id.equals(customer.id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        }
        for (com.ayan.shop.model.PurchaseItem item : purchase.items) {
            productsRepo.findById(item.productId).ifPresent(p -> {
                p.stock += item.quantity;
                productsRepo.save(p);
            });
        }
        purchase.paymentStatus = "CANCELLED";
        purchase.status = "CANCELLED";
        history.save(new OrderStatusHistory(purchase, "CANCELLED", "Order cancelled by customer"));
        return purchases.save(purchase);
    }
}
