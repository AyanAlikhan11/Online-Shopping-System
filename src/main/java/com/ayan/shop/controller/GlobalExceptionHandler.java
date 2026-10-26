package com.ayan.shop.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound(Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Page not found");
        model.addAttribute("message", "The product, order or customer you requested does not exist.");
        return "error/404";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String badRequest(IllegalArgumentException exception, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("error", "Invalid request");
        model.addAttribute("message", exception.getMessage());
        return "error/400";
    }
}
