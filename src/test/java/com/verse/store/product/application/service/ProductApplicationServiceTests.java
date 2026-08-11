package com.verse.store.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.verse.store.product.application.command.CreateProductCommand;
import com.verse.store.product.application.command.CreateProductImageCommand;
import com.verse.store.product.application.command.CreateProductVariantCommand;
import com.verse.store.product.application.command.UpdateProductCommand;
import com.verse.store.product.application.command.UpdateProductImageCommand;
import com.verse.store.product.application.command.UpdateProductVariantCommand;
import com.verse.store.product.application.exception.InvalidProductStateException;
import com.verse.store.product.application.exception.ProductNotFoundException;
import com.verse.store.product.application.mapper.ProductMapper;
import com.verse.store.product.application.model.ProductResult;
import com.verse.store.product.application.query.ProductAdminQuery;
import com.verse.store.product.application.query.ProductSortDirection;
import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductImage;
import com.verse.store.product.domain.ProductStatus;
import com.verse.store.product.domain.ProductVariant;
import com.verse.store.product.infrastructure.persistence.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTests {

    private static final UUID PRODUCT_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCommandValidator commandValidator;

    @Mock
    private ProductSlugGenerator slugGenerator;

    private ProductApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ProductApplicationService(
                productRepository, new ProductMapper(), commandValidator, slugGenerator);
        lenient().when(productRepository.saveAndFlush(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsDraftProduct() {
        when(slugGenerator.uniqueSlug("Cămașă Nouă")).thenReturn("camasa-noua");

        ProductResult result = service.createProduct(command("Cămașă Nouă"));

        assertThat(result.status()).isEqualTo(ProductStatus.DRAFT);
        verify(productRepository).saveAndFlush(any(Product.class));
    }

    @Test
    void createsProductWithVariantsAndImages() {
        when(slugGenerator.uniqueSlug(any())).thenReturn("new-product");

        ProductResult result = service.createProduct(command("New Product"));

        assertThat(result.variants()).singleElement()
                .extracting(variant -> variant.sku())
                .isEqualTo("APP-SKU-1");
        assertThat(result.images()).singleElement()
                .extracting(image -> image.primaryImage())
                .isEqualTo(true);
    }

    @Test
    void updatePreservesStatusAndSlug() {
        Product active = activeProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(active));
        UpdateProductCommand update = new UpdateProductCommand(
                PRODUCT_ID, "Renamed Product", "Updated", "Verse", "Updated Collection",
                ProductCategory.KNITWEAR, new BigDecimal("120.00"), BigDecimal.ZERO,
                List.of(new UpdateProductVariantCommand(
                        null, "APP-SKU-1", "M", "Sand", "#D8C3A5", 6)),
                List.of(new UpdateProductImageCommand(
                        null, "https://images.example.com/app-product.jpg", "Product", 0, true)));

        ProductResult result = service.updateProduct(update);

        assertThat(result.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(result.slug()).isEqualTo("existing-product");
        assertThat(result.name()).isEqualTo("Renamed Product");
    }

    @Test
    void publishesValidProduct() {
        Product draft = draftProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(draft));

        ProductResult result = service.publishProduct(PRODUCT_ID);

        assertThat(result.status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void translatesInvalidPublishRuleWithoutHidingCause() {
        Product invalid = bareProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(invalid));

        assertThatThrownBy(() -> service.publishProduct(PRODUCT_ID))
                .isInstanceOf(InvalidProductStateException.class)
                .hasMessageContaining("variant")
                .hasCauseInstanceOf(IllegalStateException.class);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void archivesProduct() {
        Product product = activeProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductResult result = service.archiveProduct(PRODUCT_ID);

        assertThat(result.status()).isEqualTo(ProductStatus.ARCHIVED);
    }

    @Test
    void deletesDraftProduct() {
        Product draft = draftProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(draft));

        service.deleteProduct(PRODUCT_ID);

        verify(productRepository).delete(draft);
    }

    @Test
    void deletesActiveProduct() {
        Product active = activeProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(active));

        service.deleteProduct(PRODUCT_ID);

        verify(productRepository).delete(active);
    }

    @Test
    void deletesArchivedProduct() {
        Product archived = activeProduct();
        archived.archive();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(archived));

        service.deleteProduct(PRODUCT_ID);

        verify(productRepository).delete(archived);
    }

    @Test
    void republishesArchivedProduct() {
        Product product = activeProduct();
        product.archive();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductResult result = service.publishProduct(PRODUCT_ID);

        assertThat(result.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(result.slug()).isEqualTo("existing-product");
    }

    @Test
    void reportsMissingProduct() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProductForAdmin(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(PRODUCT_ID.toString());
    }

    @Test
    void listsWithPaginationFiltersAndWhitelistedSort() {
        Product product = activeProduct();
        when(productRepository.findByStatusAndCategory(
                any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        ProductAdminQuery query = new ProductAdminQuery(
                2, 10, ProductStatus.ACTIVE, ProductCategory.TOPS,
                "createdAt", ProductSortDirection.DESC);

        Page<ProductResult> result = service.listProductsForAdmin(query);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findByStatusAndCategory(
                org.mockito.ArgumentMatchers.eq(ProductStatus.ACTIVE),
                org.mockito.ArgumentMatchers.eq(ProductCategory.TOPS), pageable.capture());
        assertThat(result.getContent()).hasSize(1);
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void mapsFinalPriceAndTotalStockInsideUseCase() {
        Product product = new Product(
                "Mapped Product", "mapped-product", null, "Verse", "Mapped",
                ProductCategory.TOPS, new BigDecimal("100.00"), new BigDecimal("25.00"));
        product.addVariant(new ProductVariant("MAP-1", "S", "Ink", null, 3));
        product.addVariant(new ProductVariant("MAP-2", "M", "Ink", null, 7));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductResult result = service.getProductForAdmin(PRODUCT_ID);

        assertThat(result.finalPrice()).isEqualByComparingTo("75.00");
        assertThat(result.totalStock()).isEqualTo(10);
    }

    private static CreateProductCommand command(String name) {
        return new CreateProductCommand(
                name, "Description", "Verse", "Application Collection",
                ProductCategory.TOPS, new BigDecimal("100.00"), new BigDecimal("10.00"),
                variants(), images());
    }

    private static List<CreateProductVariantCommand> variants() {
        return List.of(new CreateProductVariantCommand("APP-SKU-1", "M", "Sand", "#D8C3A5", 6));
    }

    private static List<CreateProductImageCommand> images() {
        return List.of(new CreateProductImageCommand(
                "https://images.example.com/app-product.jpg", "Product", 0, true));
    }

    private static Product bareProduct() {
        return new Product(
                "Existing Product", "existing-product", null, "Verse", "Existing",
                ProductCategory.TOPS, new BigDecimal("100.00"), BigDecimal.ZERO);
    }

    private static Product draftProduct() {
        Product product = bareProduct();
        product.addVariant(new ProductVariant("EXISTING-SKU", "M", "Ink", null, 5));
        product.addImage(new ProductImage("https://images.example.com/existing.jpg", null, 0, true));
        return product;
    }

    private static Product activeProduct() {
        Product product = draftProduct();
        product.publish();
        return product;
    }
}
