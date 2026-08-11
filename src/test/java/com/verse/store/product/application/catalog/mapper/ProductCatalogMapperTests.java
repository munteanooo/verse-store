package com.verse.store.product.application.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.verse.store.product.application.catalog.model.CatalogProductDetails;
import com.verse.store.product.application.catalog.model.CatalogProductSummary;
import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductImage;
import com.verse.store.product.domain.ProductVariant;

class ProductCatalogMapperTests {

    private final ProductCatalogMapper mapper = new ProductCatalogMapper();

    @Test
    void choosesExplicitPrimaryImage() {
        Product product = product();
        product.addImage(new ProductImage("https://images.example.com/first.jpg", "First", 0, false));
        product.addImage(new ProductImage("https://images.example.com/primary.jpg", "Primary", 5, true));

        CatalogProductSummary result = mapper.toSummary(product);

        assertThat(result.primaryImageUrl()).endsWith("primary.jpg");
        assertThat(result.primaryImageAltText()).isEqualTo("Primary");
    }

    @Test
    void fallsBackToFirstOrderedImage() {
        Product product = product();
        product.addImage(new ProductImage("https://images.example.com/later.jpg", "Later", 5, false));
        product.addImage(new ProductImage("https://images.example.com/first.jpg", "First", 1, false));

        CatalogProductDetails result = mapper.toDetails(product);

        assertThat(result.primaryImageUrl()).endsWith("first.jpg");
        assertThat(result.images()).extracting(image -> image.displayOrder()).containsExactly(1, 5);
    }

    @Test
    void handlesProductWithoutImages() {
        CatalogProductSummary result = mapper.toSummary(product());

        assertThat(result.primaryImageUrl()).isNull();
        assertThat(result.primaryImageAltText()).isNull();
    }

    @Test
    void marksOutOfStockVariantUnavailable() {
        Product product = product();
        product.addVariant(new ProductVariant("OUT-OF-STOCK", "L", "Ink", "#111111", 0));

        CatalogProductDetails result = mapper.toDetails(product);

        assertThat(result.variants()).singleElement().satisfies(variant -> {
            assertThat(variant.available()).isFalse();
            assertThat(variant.stockQuantity()).isZero();
        });
        assertThat(result.available()).isFalse();
    }

    private static Product product() {
        return new Product(
                "Catalog Product", "catalog-product", "Description", "Verse",
                "Catalog Collection", ProductCategory.TOPS,
                new BigDecimal("80.00"), BigDecimal.ZERO);
    }
}
