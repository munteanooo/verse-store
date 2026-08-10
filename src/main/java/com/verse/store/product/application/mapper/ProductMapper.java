package com.verse.store.product.application.mapper;

import org.springframework.stereotype.Component;

import com.verse.store.product.application.model.ProductImageResult;
import com.verse.store.product.application.model.ProductResult;
import com.verse.store.product.application.model.ProductVariantResult;
import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductImage;
import com.verse.store.product.domain.ProductVariant;

@Component
public class ProductMapper {

    public ProductResult toResult(Product product) {
        return new ProductResult(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getBrand(),
                product.getCollectionName(),
                product.getCategory(),
                product.getBasePrice(),
                product.getDiscountPercentage(),
                product.calculateFinalPrice(),
                product.getStatus(),
                product.totalStock(),
                product.getVariants().stream().map(this::toResult).toList(),
                product.getImages().stream().map(this::toResult).toList(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    private ProductVariantResult toResult(ProductVariant variant) {
        return new ProductVariantResult(
                variant.getId(),
                variant.getSku(),
                variant.getSize(),
                variant.getColorName(),
                variant.getColorHex(),
                variant.getStockQuantity(),
                variant.getCreatedAt(),
                variant.getUpdatedAt());
    }

    private ProductImageResult toResult(ProductImage image) {
        return new ProductImageResult(
                image.getId(),
                image.getUrl(),
                image.getAltText(),
                image.getDisplayOrder(),
                image.isPrimaryImage(),
                image.getCreatedAt());
    }
}
