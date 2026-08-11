package com.verse.store.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductDomainTests {

    @Test
    void calculatesPriceWithoutDiscount() {
        Product product = productWithDiscount("0");

        assertThat(product.calculateFinalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculatesDiscountedPriceWithMonetaryRounding() {
        Product product = productWithDiscount("15.555");

        assertThat(product.calculateFinalPrice()).isEqualByComparingTo("84.45");
    }

    @Test
    void rejectsDiscountOutsideAllowedRange() {
        assertThatThrownBy(() -> productWithDiscount("-0.01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> productWithDiscount("100.01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPublishingWithoutVariants() {
        Product product = productWithDiscount("0");
        product.addImage(primaryImage());

        assertThatThrownBy(product::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("variant");
    }

    @Test
    void rejectsPublishingWithoutPrimaryImage() {
        Product product = productWithDiscount("0");
        product.addVariant(variant("DOMAIN-SKU-1", 4));
        product.addImage(new ProductImage("https://images.example.com/secondary.jpg", "Secondary", 1, false));

        assertThatThrownBy(product::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("primary image");
    }

    @Test
    void publishesValidDraftProduct() {
        Product product = productWithDiscount("0");
        ProductVariant variant = variant("DOMAIN-SKU-2", 4);
        ProductImage image = primaryImage();
        product.addVariant(variant);
        product.addImage(image);

        product.publish();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(variant.getProduct()).isSameAs(product);
        assertThat(image.getProduct()).isSameAs(product);
    }

    @Test
    void republishesArchivedProductWithoutChangingSlug() {
        Product product = productWithDiscount("0");
        product.addVariant(variant("DOMAIN-SKU-REPUBLISH", 4));
        product.addImage(primaryImage());
        product.publish();
        product.archive();
        String slug = product.getSlug();

        product.publish();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getSlug()).isEqualTo(slug);
    }

    @Test
    void publishAndArchiveAreIdempotentInTheirTargetState() {
        Product product = productWithDiscount("0");
        product.addVariant(variant("DOMAIN-SKU-IDEMPOTENT", 4));
        product.addImage(primaryImage());

        product.publish();
        product.publish();
        product.archive();
        product.archive();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
    }

    @Test
    void totalsStockAcrossVariants() {
        Product product = productWithDiscount("0");
        product.addVariant(variant("DOMAIN-SKU-3", 7));
        product.addVariant(new ProductVariant("DOMAIN-SKU-4", "L", "Ink", "#111111", 5));

        assertThat(product.totalStock()).isEqualTo(12);
    }

    private static Product productWithDiscount(String discount) {
        return new Product(
                "Domain Product",
                "domain-product",
                "A product used by domain tests.",
                "Verse",
                "Domain Collection",
                ProductCategory.TOPS,
                new BigDecimal("100.00"),
                new BigDecimal(discount));
    }

    private static ProductVariant variant(String sku, int stock) {
        return new ProductVariant(sku, "M", "Sand", "#D8C3A5", stock);
    }

    private static ProductImage primaryImage() {
        return new ProductImage("https://images.example.com/primary.jpg", "Primary", 0, true);
    }
}
