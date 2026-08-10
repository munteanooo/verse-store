package com.verse.store.product.application.exception;

import java.util.List;

public class ProductValidationException extends RuntimeException {

    private final List<String> violations;

    public ProductValidationException(List<String> violations) {
        super(String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
