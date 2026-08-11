package com.verse.store.product.application.catalog.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.verse.store.product.domain.ProductCategory;

public record CatalogProductDetails(
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
        String primaryImageUrl,
        String primaryImageAltText,
        boolean available,
        int totalStock,
        List<CatalogProductVariant> variants,
        List<CatalogProductImage> images) {

    public CatalogProductDetails {
        variants = List.copyOf(variants);
        images = List.copyOf(images);
    }
}
