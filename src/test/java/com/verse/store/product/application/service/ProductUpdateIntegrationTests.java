package com.verse.store.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.verse.store.TestcontainersConfiguration;
import com.verse.store.product.application.command.UpdateProductCommand;
import com.verse.store.product.application.command.UpdateProductImageCommand;
import com.verse.store.product.application.command.UpdateProductVariantCommand;
import com.verse.store.product.application.exception.DuplicateProductException;
import com.verse.store.product.application.exception.ProductValidationException;
import com.verse.store.product.application.model.ProductImageResult;
import com.verse.store.product.application.model.ProductResult;
import com.verse.store.product.application.model.ProductVariantResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductUpdateIntegrationTests {

    private static final UUID PRODUCT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_PRODUCT_VARIANT_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000004");

    @Autowired
    private ProductApplicationService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void legacyReplacementWithTheSameSkuViolatesPostgresSkuConstraint() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO product_variants
                    (id, product_id, sku, size, color_name, stock_quantity, created_at, updated_at)
                VALUES (?, ?, 'VRS-HZN-S-SAND', 'XL', 'Test', 1, now(), now())
                """, UUID.fromString("99000000-0000-0000-0000-000000000001"), PRODUCT_ID))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(error -> assertThat(constraintName(error))
                        .isEqualTo("product_variants_sku_key"));
    }

    @Test
    void unchangedUpdateKeepsChildIdsSlugAndStatus() {
        ProductResult before = service.getProductForAdmin(PRODUCT_ID);

        ProductResult after = service.updateProduct(command(before, variants(before), images(before)));

        assertThat(after.slug()).isEqualTo(before.slug());
        assertThat(after.status()).isEqualTo(before.status());
        assertThat(after.variants()).extracting(ProductVariantResult::id)
                .containsExactlyElementsOf(before.variants().stream().map(ProductVariantResult::id).toList());
        assertThat(after.images()).extracting(ProductImageResult::id)
                .containsExactlyElementsOf(before.images().stream().map(ProductImageResult::id).toList());
    }

    @Test
    void editingArchivedProductPreservesArchivedStatusAndSlug() {
        service.archiveProduct(PRODUCT_ID);
        ProductResult archived = service.getProductForAdmin(PRODUCT_ID);
        UpdateProductCommand update = new UpdateProductCommand(
                archived.id(), "Archived product edited", archived.description(), archived.brand(),
                archived.collectionName(), archived.category(), archived.basePrice(),
                archived.discountPercentage(), variants(archived), images(archived));

        ProductResult result = service.updateProduct(update);

        assertThat(result.status()).isEqualTo(com.verse.store.product.domain.ProductStatus.ARCHIVED);
        assertThat(result.slug()).isEqualTo(archived.slug());
        assertThat(result.name()).isEqualTo("Archived product edited");
    }

    @Test
    void updatesOnlyNameDescriptionPriceAndImageWithoutDuplicate() {
        ProductResult before = service.getProductForAdmin(PRODUCT_ID);
        List<UpdateProductImageCommand> images = images(before);
        ProductImageResult first = before.images().getFirst();
        images.set(0, image(first, "https://images.example.com/changed.jpg", first.primaryImage()));

        UpdateProductCommand command = new UpdateProductCommand(
                before.id(), "Changed name", "Changed description", before.brand(),
                before.collectionName(), before.category(), new BigDecimal("88.40"),
                before.discountPercentage(), variants(before), images);
        ProductResult after = service.updateProduct(command);

        assertThat(after.name()).isEqualTo("Changed name");
        assertThat(after.description()).isEqualTo("Changed description");
        assertThat(after.basePrice()).isEqualByComparingTo("88.40");
        assertThat(after.images().getFirst().url()).isEqualTo("https://images.example.com/changed.jpg");
        assertThat(after.variants().getFirst().sku()).isEqualTo(before.variants().getFirst().sku());
    }

    @Test
    void updatesStockAndSkuInPlaceAndCanAddAndRemoveVariant() {
        ProductResult before = service.getProductForAdmin(PRODUCT_ID);
        ProductVariantResult retained = before.variants().getFirst();
        List<UpdateProductVariantCommand> variants = new ArrayList<>();
        variants.add(new UpdateProductVariantCommand(
                retained.id(), "VRS-HZN-S-FREE", retained.size(), retained.colorName(),
                retained.colorHex(), retained.stockQuantity() + 7));
        variants.add(new UpdateProductVariantCommand(
                null, "VRS-HZN-XL-NEW", "XL", "Night", "#111111", 4));

        ProductResult after = service.updateProduct(command(before, variants, images(before)));

        assertThat(after.variants()).hasSize(2);
        assertThat(after.variants()).filteredOn(item -> item.id().equals(retained.id())).singleElement()
                .satisfies(item -> {
                    assertThat(item.sku()).isEqualTo("VRS-HZN-S-FREE");
                    assertThat(item.stockQuantity()).isEqualTo(retained.stockQuantity() + 7);
                });
        assertThat(after.variants()).extracting(ProductVariantResult::sku)
                .contains("VRS-HZN-XL-NEW")
                .doesNotContain(before.variants().get(1).sku());
    }

    @Test
    void rejectsSkuOwnedByAnotherProduct() {
        ProductResult before = service.getProductForAdmin(PRODUCT_ID);
        ProductVariantResult first = before.variants().getFirst();
        List<UpdateProductVariantCommand> variants = variants(before);
        variants.set(0, new UpdateProductVariantCommand(
                first.id(), "VRS-DRF-38-INK", first.size(), first.colorName(),
                first.colorHex(), first.stockQuantity()));

        assertThatThrownBy(() -> service.updateProduct(command(before, variants, images(before))))
                .isInstanceOf(DuplicateProductException.class)
                .hasMessage("A product variant with this SKU already exists");
    }

    @Test
    void rejectsDuplicateOptionAndMultiplePrimaryImagesBeforePersistence() {
        ProductResult before = service.getProductForAdmin(PRODUCT_ID);
        ProductVariantResult first = before.variants().getFirst();
        List<UpdateProductVariantCommand> variants = variants(before);
        ProductVariantResult second = before.variants().get(1);
        variants.set(1, new UpdateProductVariantCommand(
                second.id(), second.sku(), first.size(), first.colorName(),
                second.colorHex(), second.stockQuantity()));

        assertThatThrownBy(() -> service.updateProduct(command(before, variants, images(before))))
                .isInstanceOf(ProductValidationException.class)
                .hasMessageContaining("duplicate size and color combination");

        List<UpdateProductImageCommand> images = images(before).stream()
                .map(image -> new UpdateProductImageCommand(
                        image.id(), image.url(), image.altText(), image.displayOrder(), true))
                .toList();
        assertThatThrownBy(() -> service.updateProduct(command(before, variants(before), images)))
                .isInstanceOf(ProductValidationException.class)
                .hasMessageContaining("at most one primary image");
    }

    @Test
    void switchesPrimaryImageWithoutPartialUniqueIndexConflict() {
        ProductResult before = service.getProductForAdmin(PRODUCT_ID);
        ProductImageResult oldPrimary = before.images().stream()
                .filter(ProductImageResult::primaryImage).findFirst().orElseThrow();
        ProductImageResult nextPrimary = before.images().stream()
                .filter(image -> !image.primaryImage()).findFirst().orElseThrow();
        List<UpdateProductImageCommand> images = before.images().stream()
                .map(image -> image(image, image.url(), image.id().equals(nextPrimary.id())))
                .toList();

        ProductResult after = service.updateProduct(command(before, variants(before), images));

        assertThat(after.images()).filteredOn(ProductImageResult::primaryImage).singleElement()
                .extracting(ProductImageResult::id).isEqualTo(nextPrimary.id());
        assertThat(after.images()).filteredOn(image -> image.id().equals(oldPrimary.id())).singleElement()
                .extracting(ProductImageResult::primaryImage).isEqualTo(false);
    }

    @Test
    void rejectsVariantIdOwnedByAnotherProduct() {
        ProductResult before = service.getProductForAdmin(PRODUCT_ID);
        ProductVariantResult first = before.variants().getFirst();
        List<UpdateProductVariantCommand> variants = variants(before);
        variants.set(0, new UpdateProductVariantCommand(
                OTHER_PRODUCT_VARIANT_ID, first.sku(), first.size(), first.colorName(),
                first.colorHex(), first.stockQuantity()));

        assertThatThrownBy(() -> service.updateProduct(command(before, variants, images(before))))
                .isInstanceOf(ProductValidationException.class)
                .hasMessageContaining("does not belong to this product");
    }

    private static UpdateProductCommand command(
            ProductResult product,
            List<UpdateProductVariantCommand> variants,
            List<UpdateProductImageCommand> images) {
        return new UpdateProductCommand(
                product.id(), product.name(), product.description(), product.brand(),
                product.collectionName(), product.category(), product.basePrice(),
                product.discountPercentage(), variants, images);
    }

    private static List<UpdateProductVariantCommand> variants(ProductResult product) {
        return new ArrayList<>(product.variants().stream().map(variant -> new UpdateProductVariantCommand(
                variant.id(), variant.sku(), variant.size(), variant.colorName(),
                variant.colorHex(), variant.stockQuantity())).toList());
    }

    private static List<UpdateProductImageCommand> images(ProductResult product) {
        return new ArrayList<>(product.images().stream()
                .map(image -> image(image, image.url(), image.primaryImage())).toList());
    }

    private static UpdateProductImageCommand image(
            ProductImageResult image, String url, boolean primary) {
        return new UpdateProductImageCommand(
                image.id(), url, image.altText(), image.displayOrder(), primary);
    }

    private static String constraintName(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.postgresql.util.PSQLException postgresException) {
                return postgresException.getServerErrorMessage().getConstraint();
            }
        }
        return null;
    }
}
