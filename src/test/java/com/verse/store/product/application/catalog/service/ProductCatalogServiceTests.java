package com.verse.store.product.application.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.verse.store.product.application.catalog.exception.CatalogProductNotFoundException;
import com.verse.store.product.application.catalog.mapper.ProductCatalogMapper;
import com.verse.store.product.application.catalog.model.CatalogProductDetails;
import com.verse.store.product.application.catalog.model.CatalogProductSummary;
import com.verse.store.product.application.catalog.query.CatalogProductQuery;
import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductImage;
import com.verse.store.product.domain.ProductStatus;
import com.verse.store.product.domain.ProductVariant;
import com.verse.store.product.infrastructure.persistence.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CatalogQueryValidator queryValidator;

    private ProductCatalogService service;

    @BeforeEach
    void setUp() {
        service = new ProductCatalogService(
                productRepository, new ProductCatalogMapper(), queryValidator);
    }

    @Test
    void returnsOnlyActiveProductsFromCatalogQuery() {
        Product active = activeProduct();
        when(productRepository.findActiveCatalogProducts(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active)));

        Page<CatalogProductSummary> result = service.listActiveProducts(query(null, null));

        assertThat(result.getContent()).singleElement()
                .extracting(CatalogProductSummary::slug)
                .isEqualTo("active-product");
        assertThat(active.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void draftProductsAreHiddenFromCatalogList() {
        when(productRepository.findActiveCatalogProducts(any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThat(service.listActiveProducts(query(null, null))).isEmpty();
    }

    @Test
    void draftSlugIsReportedAsNotFound() {
        when(productRepository.findActiveCatalogProductBySlug("draft-product"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActiveProductBySlug("draft-product"))
                .isInstanceOf(CatalogProductNotFoundException.class);
    }

    @Test
    void archivedSlugIsReportedAsNotFound() {
        when(productRepository.findActiveCatalogProductBySlug("archived-product"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActiveProductBySlug("archived-product"))
                .isInstanceOf(CatalogProductNotFoundException.class);
    }

    @Test
    void passesCategoryFilterToActiveQuery() {
        when(productRepository.findActiveCatalogProducts(any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listActiveProducts(query(ProductCategory.TOPS, null));

        verify(productRepository).findActiveCatalogProducts(
                org.mockito.ArgumentMatchers.eq(ProductCategory.TOPS),
                org.mockito.ArgumentMatchers.isNull(), any(Pageable.class));
    }

    @Test
    void trimsAndPassesCollectionFilter() {
        when(productRepository.findActiveCatalogProducts(any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listActiveProducts(query(null, "  Coastal Lines  "));

        verify(productRepository).findActiveCatalogProducts(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("Coastal Lines"), any(Pageable.class));
    }

    @Test
    void combinesCategoryAndCollectionWithStableSort() {
        when(productRepository.findActiveCatalogProducts(any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listActiveProducts(query(ProductCategory.TOPS, "Coastal Lines"));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findActiveCatalogProducts(
                org.mockito.ArgumentMatchers.eq(ProductCategory.TOPS),
                org.mockito.ArgumentMatchers.eq("Coastal Lines"), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageable.getValue().getSort().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    void returnsActiveProductBySlug() {
        Product active = activeProduct();
        when(productRepository.findActiveCatalogProductBySlug("active-product"))
                .thenReturn(Optional.of(active));

        CatalogProductDetails result = service.getActiveProductBySlug("active-product");

        assertThat(result.slug()).isEqualTo("active-product");
        assertThat(result.finalPrice()).isEqualByComparingTo("90.00");
    }

    @Test
    void returnsDistinctActiveCollectionsFromRepository() {
        when(productRepository.findDistinctActiveCollectionNames())
                .thenReturn(List.of("Coastal Lines", "Quiet Terrain"));

        assertThat(service.listActiveCollections())
                .containsExactly("Coastal Lines", "Quiet Terrain");
    }

    private static CatalogProductQuery query(ProductCategory category, String collection) {
        return new CatalogProductQuery(0, 20, category, collection);
    }

    private static Product activeProduct() {
        Product product = new Product(
                "Active Product", "active-product", "Public description", "Verse",
                "Coastal Lines", ProductCategory.TOPS,
                new BigDecimal("100.00"), new BigDecimal("10.00"));
        product.addVariant(new ProductVariant("CATALOG-SKU", "M", "Sand", null, 4));
        product.addImage(new ProductImage(
                "https://images.example.com/active.jpg", "Active", 0, true));
        product.publish();
        return product;
    }
}
