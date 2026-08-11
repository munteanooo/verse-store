package com.verse.store.product.api.admin.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;

public record AdminProductResponse(
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
        List<AdminProductVariantResponse> variants,
        List<AdminProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt) {

    public AdminProductResponse {
        variants = List.copyOf(variants);
        images = List.copyOf(images);
    }
}
