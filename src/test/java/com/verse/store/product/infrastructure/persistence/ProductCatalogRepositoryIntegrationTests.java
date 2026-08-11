package com.verse.store.product.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.verse.store.TestcontainersConfiguration;
import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductCatalogRepositoryIntegrationTests {

    private static final PageRequest CATALOG_PAGE = PageRequest.of(
            0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));

    @Autowired
    private ProductRepository productRepository;

    @Test
    void flywaySeedContainsActiveAndDraftProducts() {
        assertThat(productRepository.findByStatus(ProductStatus.ACTIVE, CATALOG_PAGE).getTotalElements())
                .isEqualTo(4);
        assertThat(productRepository.findByStatus(ProductStatus.DRAFT, CATALOG_PAGE).getTotalElements())
                .isEqualTo(2);
    }

    @Test
    void catalogQueryCannotReturnDraftProducts() {
        Page<Product> products = productRepository.findActiveCatalogProducts(null, null, CATALOG_PAGE);

        assertThat(products.getContent()).hasSize(4)
                .allMatch(product -> product.getStatus() == ProductStatus.ACTIVE)
                .extracting(Product::getSlug)
                .doesNotContain("moss-rib-cardigan", "contour-canvas-tote");
    }

    @Test
    void combinesCategoryAndCollectionFiltersOnPostgresql() {
        Page<Product> products = productRepository.findActiveCatalogProducts(
                ProductCategory.TOPS, "Coastal Lines", CATALOG_PAGE);
        List<Product> content = products.getContent();
        List<java.util.UUID> ids = content.stream().map(Product::getId).toList();
        productRepository.loadCatalogVariants(ids);
        productRepository.loadCatalogImages(ids);

        assertThat(content).singleElement().satisfies(product -> {
            assertThat(product.getSlug()).isEqualTo("linen-horizon-shirt");
            assertThat(product.getVariants()).hasSize(3);
            assertThat(product.getImages()).hasSize(2);
        });
    }
}
