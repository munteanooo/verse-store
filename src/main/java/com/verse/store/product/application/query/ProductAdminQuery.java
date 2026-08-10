package com.verse.store.product.application.query;

import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductAdminQuery(
        @Min(0) int page,
        @Min(1) @Max(100) int size,
        ProductStatus status,
        ProductCategory category,
        @NotBlank String sortBy,
        @NotNull ProductSortDirection direction) {
}
