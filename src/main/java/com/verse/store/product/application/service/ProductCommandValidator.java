package com.verse.store.product.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.net.URI;

import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

import com.verse.store.product.application.command.CreateProductCommand;
import com.verse.store.product.application.command.CreateProductImageCommand;
import com.verse.store.product.application.command.CreateProductVariantCommand;
import com.verse.store.product.application.command.UpdateProductCommand;
import com.verse.store.product.application.command.UpdateProductImageCommand;
import com.verse.store.product.application.command.UpdateProductVariantCommand;
import com.verse.store.product.application.exception.ProductValidationException;
import com.verse.store.product.application.query.ProductAdminQuery;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
public class ProductCommandValidator {

    private final Validator validator;
    private final boolean localUrlsAllowed;

    public ProductCommandValidator(Validator validator, Environment environment) {
        this.validator = validator;
        this.localUrlsAllowed = environment.matchesProfiles("local", "docker");
    }

    public void validate(CreateProductCommand command) {
        validateBean(command);
        validateCollections(command.variants(), command.images());
    }

    public void validate(UpdateProductCommand command) {
        validateBean(command);
        validateUpdateCollections(command.variants(), command.images());
    }

    private void validateUpdateCollections(
            List<UpdateProductVariantCommand> variants,
            List<UpdateProductImageCommand> images) {
        validateUniqueness(
                variants.stream().map(variant -> new VariantValues(
                        variant.sku(), variant.size(), variant.colorName())).toList(),
                images.stream().filter(UpdateProductImageCommand::primaryImage).count());
        validateImageUrls(images.stream().map(UpdateProductImageCommand::url).toList());
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
        validateUniqueness(
                variants.stream().map(variant -> new VariantValues(
                        variant.sku(), variant.size(), variant.colorName())).toList(),
                images.stream().filter(CreateProductImageCommand::primaryImage).count());
        validateImageUrls(images.stream().map(CreateProductImageCommand::url).toList());
    }

    private void validateImageUrls(List<String> urls) {
        List<String> violations = new ArrayList<>();
        for (String value : urls) {
            if (value == null || value.length() > 2048) {
                violations.add("image URL must not exceed 2048 characters");
                continue;
            }
            if (value.startsWith("/media/products/")) continue;
            try {
                URI uri = URI.create(value);
                boolean allowed = "https".equalsIgnoreCase(uri.getScheme())
                        || (localUrlsAllowed && "http".equalsIgnoreCase(uri.getScheme()));
                if (!allowed || uri.getHost() == null) {
                    violations.add("image URL must use HTTPS");
                }
            } catch (IllegalArgumentException exception) {
                violations.add("image URL is invalid");
            }
        }
        if (!violations.isEmpty()) throw new ProductValidationException(violations);
    }

    private void validateUniqueness(List<VariantValues> variants, long primaryImages) {
        List<String> violations = new ArrayList<>();
        Set<String> skus = new HashSet<>();
        Set<String> options = new HashSet<>();

        for (VariantValues variant : variants) {
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

    private record VariantValues(String sku, String size, String colorName) {
    }
}
