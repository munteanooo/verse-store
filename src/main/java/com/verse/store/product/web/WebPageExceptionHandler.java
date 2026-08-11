package com.verse.store.product.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.verse.store.product.application.catalog.exception.CatalogProductNotFoundException;
import com.verse.store.product.application.exception.ProductNotFoundException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = {CatalogPageController.class, AdminProductPageController.class})
public class WebPageExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({CatalogProductNotFoundException.class, ProductNotFoundException.class})
    public String notFound(Model model) {
        model.addAttribute("message", "The product you requested could not be found.");
        return "error/404";
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String unexpectedError() {
        return "error/500";
    }
}
