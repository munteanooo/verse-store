package com.verse.store.product.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.verse.store.product.application.command.CreateProductCommand;
import com.verse.store.product.application.command.CreateProductImageCommand;
import com.verse.store.product.application.command.CreateProductVariantCommand;
import com.verse.store.product.application.command.UpdateProductCommand;
import com.verse.store.product.application.exception.ProductValidationException;
import com.verse.store.product.application.query.ProductAdminQuery;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
public class ProductCommandValidator {

    private final Validator validator;

    public ProductCommandValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(CreateProductCommand command) {
        validateBean(command);
        validateCollections(command.variants(), command.images());
    }

    public void validate(UpdateProductCommand command) {
        validateBean(command);
        validateCollections(command.variants(), command.images());
    }

    public void validate(ProductAdminQuery query) {
        validateBean(query);
    }

    private <T> void validateBean(T command) {
        if (command == null) {
            throw new ProductValidationException(List.of("command must not be null"));
        }
        List<String> violations = validator.validate(command).stream()
                .sorted((left, right) -> left.getPropertyPath().toString()
                        .compareTo(right.getPropertyPath().toString()))
                .map(this::message)
                .toList();
        if (!violations.isEmpty()) {
            throw new ProductValidationException(violations);
        }
    }

    private void validateCollections(
            List<CreateProductVariantCommand> variants,
            List<CreateProductImageCommand> images) {
        List<String> violations = new ArrayList<>();
        Set<String> skus = new HashSet<>();
        Set<String> options = new HashSet<>();

        for (CreateProductVariantCommand variant : variants) {
            String normalizedSku = normalize(variant.sku());
            if (!skus.add(normalizedSku)) {
                violations.add("duplicate SKU in product: " + variant.sku());
            }
            String option = normalize(variant.size()) + "\u0000" + normalize(variant.colorName());
            if (!options.add(option)) {
                violations.add("duplicate size and color combination: "
                        + variant.size() + " / " + variant.colorName());
            }
        }

        long primaryImages = images.stream().filter(CreateProductImageCommand::primaryImage).count();
        if (primaryImages > 1) {
            violations.add("a product can have at most one primary image");
        }
        if (!violations.isEmpty()) {
            throw new ProductValidationException(violations);
        }
    }

    private String message(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
