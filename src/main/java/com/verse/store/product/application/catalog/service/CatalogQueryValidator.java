package com.verse.store.product.application.catalog.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.verse.store.product.application.catalog.query.CatalogProductQuery;
import com.verse.store.product.application.exception.ProductValidationException;

import jakarta.validation.Validator;

@Component
public class CatalogQueryValidator {

    private final Validator validator;

    public CatalogQueryValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(CatalogProductQuery query) {
        if (query == null) {
            throw new ProductValidationException(List.of("catalog query must not be null"));
        }
        List<String> violations = validator.validate(query).stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .toList();
        if (!violations.isEmpty()) {
            throw new ProductValidationException(violations);
        }
    }
}
