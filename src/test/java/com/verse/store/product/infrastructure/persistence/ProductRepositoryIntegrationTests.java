package com.verse.store.product.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.verse.store.TestcontainersConfiguration;
import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductImage;
import com.verse.store.product.domain.ProductStatus;
import com.verse.store.product.domain.ProductVariant;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductRepositoryIntegrationTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mapsFlywaySeedThroughRepository() {
        Product product = productRepository.findBySlug("linen-horizon-shirt").orElseThrow();

        assertThat(product.getId()).isEqualTo(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        assertThat(product.getCategory()).isEqualTo(ProductCategory.TOPS);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getBasePrice()).isEqualByComparingTo("79.90");
        assertThat(product.getVariants()).hasSize(3);
        assertThat(product.getImages()).hasSize(2);
        assertThat(productRepository.existsBySlug("linen-horizon-shirt")).isTrue();
        assertThat(productRepository.findDistinctActiveCollectionNames())
                .containsExactly("Coastal Lines", "Quiet Terrain");
    }

    @Test
    void savesAndReadsProductWithVariantsAndImages() {
        Product product = newProduct("persistence-product", "PERSIST-SKU-1");

        Product saved = productRepository.saveAndFlush(product);
        UUID productId = saved.getId();
        entityManager.clear();

        Product reloaded = productRepository.findById(productId).orElseThrow();
        assertThat(reloaded.getSlug()).isEqualTo("persistence-product");
        assertThat(reloaded.getVariants()).singleElement()
                .extracting(ProductVariant::getSku)
                .isEqualTo("PERSIST-SKU-1");
        assertThat(reloaded.getImages()).singleElement()
                .satisfies(image -> {
                    assertThat(image.isPrimaryImage()).isTrue();
                    assertThat(image.getProduct()).isSameAs(reloaded);
                });
    }

    @Test
    void cascadeDeletesVariantsAndImages() {
        Product saved = productRepository.saveAndFlush(newProduct("cascade-product", "PERSIST-SKU-2"));
        UUID productId = saved.getId();

        productRepository.delete(saved);
        productRepository.flush();

        Integer variantCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_variants WHERE product_id = ?", Integer.class, productId);
        Integer imageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_images WHERE product_id = ?", Integer.class, productId);
        assertThat(variantCount).isZero();
        assertThat(imageCount).isZero();
    }

    @Test
    void filtersProductsByStatusAndCategory() {
        Page<Product> activeProducts = productRepository.findByStatus(
                ProductStatus.ACTIVE, PageRequest.of(0, 20));
        Page<Product> activeTops = productRepository.findByStatusAndCategory(
                ProductStatus.ACTIVE, ProductCategory.TOPS, PageRequest.of(0, 20));

        assertThat(activeProducts.getTotalElements()).isEqualTo(4);
        assertThat(activeTops.getTotalElements()).isEqualTo(1);
        assertThat(activeTops.getContent())
                .extracting(Product::getSlug)
                .containsExactly("linen-horizon-shirt");
    }

    private static Product newProduct(String slug, String sku) {
        Product product = new Product(
                "Persistence Product",
                slug,
                "A product used by persistence tests.",
                "Verse",
                "Persistence Collection",
                ProductCategory.OUTERWEAR,
                new BigDecimal("189.90"),
                new BigDecimal("5.00"));
        product.addVariant(new ProductVariant(sku, "M", "Graphite", "#454545", 9));
        product.addImage(new ProductImage(
                "https://images.example.com/" + slug + ".jpg", "Persistence Product", 0, true));
        return product;
    }
}
