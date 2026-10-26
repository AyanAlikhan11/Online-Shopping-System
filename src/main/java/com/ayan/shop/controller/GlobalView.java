package com.ayan.shop.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
public class GlobalView {
    @ModelAttribute
    void common(Model model, Authentication auth, HttpServletRequest request) {
        boolean logged = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("loggedIn", logged);
        model.addAttribute("isAdmin", logged && auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));

        int cartCount = 0;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("cart") instanceof Map<?, ?> cart) {
            for (Object value : cart.values()) {
                if (value instanceof Integer quantity) {
                    cartCount += quantity;
                }
            }
        }
        model.addAttribute("cartCount", cartCount);
    }
}
