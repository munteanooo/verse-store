package com.verse.store.product.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;

public record ProductResult(
        UUID id,
        String name,
        String slug,
        String description,
        String brand,
        String collectionName,
        ProductCategory category,
        BigDecimal basePrice,
        BigDecimal discountPercentage,
        BigDecimal finalPrice,
        ProductStatus status,
        int totalStock,
        List<ProductVariantResult> variants,
        List<ProductImageResult> images,
        Instant createdAt,
        Instant updatedAt) {

    public ProductResult {
        variants = List.copyOf(variants);
        images = List.copyOf(images);
    }
}
