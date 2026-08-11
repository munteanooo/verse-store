package com.verse.store.product.api.catalog.mapper;

import org.springframework.stereotype.Component;

import com.verse.store.product.api.catalog.response.CatalogFilterOptionResponse;
import com.verse.store.product.api.catalog.response.CatalogProductDetailsResponse;
import com.verse.store.product.api.catalog.response.CatalogProductImageResponse;
import com.verse.store.product.api.catalog.response.CatalogProductSummaryResponse;
import com.verse.store.product.api.catalog.response.CatalogProductVariantResponse;
import com.verse.store.product.application.catalog.model.CatalogFilterOption;
import com.verse.store.product.application.catalog.model.CatalogProductDetails;
import com.verse.store.product.application.catalog.model.CatalogProductImage;
import com.verse.store.product.application.catalog.model.CatalogProductSummary;
import com.verse.store.product.application.catalog.model.CatalogProductVariant;

@Component
public class CatalogProductApiMapper {

    public CatalogProductSummaryResponse toResponse(CatalogProductSummary summary) {
        return new CatalogProductSummaryResponse(
                summary.id(), summary.name(), summary.slug(), summary.brand(),
                summary.collectionName(), summary.category(), summary.basePrice(),
                summary.discountPercentage(), summary.finalPrice(), summary.primaryImageUrl(),
                summary.primaryImageAltText(), summary.available(), summary.totalStock());
    }

    public CatalogProductDetailsResponse toResponse(CatalogProductDetails details) {
        return new CatalogProductDetailsResponse(
                details.id(), details.name(), details.slug(), details.description(), details.brand(),
                details.collectionName(), details.category(), details.basePrice(),
                details.discountPercentage(), details.finalPrice(), details.primaryImageUrl(),
                details.primaryImageAltText(), details.available(), details.totalStock(),
                details.variants().stream().map(this::toResponse).toList(),
                details.images().stream().map(this::toResponse).toList());
    }

    public CatalogFilterOptionResponse toResponse(CatalogFilterOption option) {
        return new CatalogFilterOptionResponse(option.value(), option.label());
    }

    private CatalogProductVariantResponse toResponse(CatalogProductVariant variant) {
        return new CatalogProductVariantResponse(
                variant.id(), variant.size(), variant.colorName(), variant.colorHex(),
                variant.stockQuantity(), variant.available());
    }

    private CatalogProductImageResponse toResponse(CatalogProductImage image) {
        return new CatalogProductImageResponse(
                image.id(), image.url(), image.altText(), image.displayOrder(), image.primaryImage());
    }
}
