package com.verse.store.product.api.catalog.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.verse.store.product.domain.ProductCategory;

public record CatalogProductDetailsResponse(
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
        List<CatalogProductVariantResponse> variants,
        List<CatalogProductImageResponse> images) {

    public CatalogProductDetailsResponse {
        variants = List.copyOf(variants);
        images = List.copyOf(images);
    }
}
