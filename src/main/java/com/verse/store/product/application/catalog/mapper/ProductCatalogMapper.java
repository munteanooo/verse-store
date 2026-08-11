package com.verse.store.product.application.catalog.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.verse.store.product.application.catalog.model.CatalogProductDetails;
import com.verse.store.product.application.catalog.model.CatalogProductImage;
import com.verse.store.product.application.catalog.model.CatalogProductSummary;
import com.verse.store.product.application.catalog.model.CatalogProductVariant;
import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductImage;
import com.verse.store.product.domain.ProductVariant;

@Component
public class ProductCatalogMapper {

    private static final Comparator<ProductImage> IMAGE_ORDER = Comparator
            .comparingInt(ProductImage::getDisplayOrder)
            .thenComparing(image -> image.getId() == null ? "" : image.getId().toString());

    public CatalogProductSummary toSummary(Product product) {
        ProductImage primaryImage = primaryImage(product);
        int totalStock = product.totalStock();
        return new CatalogProductSummary(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getBrand(),
                product.getCollectionName(),
                product.getCategory(),
                product.getBasePrice(),
                product.getDiscountPercentage(),
                product.calculateFinalPrice(),
                primaryImage == null ? null : primaryImage.getUrl(),
                primaryImage == null ? null : primaryImage.getAltText(),
                totalStock > 0,
                totalStock);
    }

    public CatalogProductDetails toDetails(Product product) {
        ProductImage primaryImage = primaryImage(product);
        List<CatalogProductImage> images = product.getImages().stream()
                .sorted(IMAGE_ORDER)
                .map(this::toImage)
                .toList();
        List<CatalogProductVariant> variants = product.getVariants().stream()
                .map(this::toVariant)
                .toList();
        int totalStock = product.totalStock();
        return new CatalogProductDetails(
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
                primaryImage == null ? null : primaryImage.getUrl(),
                primaryImage == null ? null : primaryImage.getAltText(),
                totalStock > 0,
                totalStock,
                variants,
                images);
    }

    private ProductImage primaryImage(Product product) {
        List<ProductImage> orderedImages = product.getImages().stream().sorted(IMAGE_ORDER).toList();
        return orderedImages.stream()
                .filter(ProductImage::isPrimaryImage)
                .findFirst()
                .orElse(orderedImages.isEmpty() ? null : orderedImages.getFirst());
    }

    private CatalogProductVariant toVariant(ProductVariant variant) {
        return new CatalogProductVariant(
                variant.getId(),
                variant.getSize(),
                variant.getColorName(),
                variant.getColorHex(),
                variant.getStockQuantity(),
                variant.getStockQuantity() > 0);
    }

    private CatalogProductImage toImage(ProductImage image) {
        return new CatalogProductImage(
                image.getId(), image.getUrl(), image.getAltText(),
                image.getDisplayOrder(), image.isPrimaryImage());
    }
}
