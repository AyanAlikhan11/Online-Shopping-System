package com.ayan.shop.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;
import java.util.NoSuchElementException;

@ControllerAdvice
public class WebAdvice {
    @ModelAttribute
    void common(Model model, Authentication auth, HttpServletRequest request) {
        boolean logged = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("loggedIn", logged);
        model.addAttribute("isAdmin", logged && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        int count = 0;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("cart") instanceof Map<?, ?> cart) {
            for (Object value : cart.values()) {
                if (value instanceof Integer qty) {
                    count += qty;
                }
            }
        }
        model.addAttribute("cartCount", count);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound(Model model) {
        return page(model, 404, "Page not found",
                "The product, order or customer you requested does not exist.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String badRequest(IllegalArgumentException exception, Model model) {
        return page(model, 400, "Invalid request", exception.getMessage());
    }

    static String page(Model model, int status, String error, String message) {
        model.addAttribute("status", status);
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        return "error";
    }
}
