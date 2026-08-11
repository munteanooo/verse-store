package com.verse.store.product.application.catalog.query;

import com.verse.store.product.domain.ProductCategory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CatalogProductQuery(
        @Min(0) int page,
        @Min(1) @Max(100) int size,
        ProductCategory category,
        String collection) {
}
