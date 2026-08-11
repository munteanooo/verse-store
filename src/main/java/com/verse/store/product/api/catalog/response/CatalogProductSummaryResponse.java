package com.verse.store.product.api.catalog.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.verse.store.product.domain.ProductCategory;

public record CatalogProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        String brand,
        String collectionName,
        ProductCategory category,
        BigDecimal basePrice,
        BigDecimal discountPercentage,
        BigDecimal finalPrice,
        String primaryImageUrl,
        String primaryImageAltText,
        boolean available,
        int totalStock) {
}
