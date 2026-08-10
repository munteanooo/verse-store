package com.verse.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ProductCatalogMigrationIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAllCatalogTables() {
        List<String> tableNames = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('products', 'product_variants', 'product_images')
                ORDER BY table_name
                """, String.class);

        assertThat(tableNames).containsExactly("product_images", "product_variants", "products");
    }

    @Test
    void appliesVersionOneMigration() {
        Integer applied = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '1' AND success = TRUE
                """, Integer.class);

        assertThat(applied).isEqualTo(1);
    }

    @Test
    void insertsDeterministicDemoProducts() {
        Integer productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        Integer collectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT collection_name) FROM products", Integer.class);
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT DISTINCT status FROM products ORDER BY status", String.class);

        assertThat(productCount).isGreaterThanOrEqualTo(6);
        assertThat(collectionCount).isGreaterThanOrEqualTo(2);
        assertThat(statuses).contains("ACTIVE", "DRAFT");
    }

    @Test
    void rejectsNegativeBasePrice() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO products (
                    id, name, slug, brand, collection_name, category,
                    base_price, created_at, updated_at
                ) VALUES (
                    '90000000-0000-0000-0000-000000000001', 'Invalid Price',
                    'invalid-negative-price', 'Verse', 'Constraint Tests', 'TOPS',
                    -0.01, '2026-03-01T00:00:00Z', '2026-03-01T00:00:00Z'
                )
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativeStock() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO product_variants (
                    id, product_id, sku, size, color_name, stock_quantity,
                    created_at, updated_at
                ) VALUES (
                    '90000000-0000-0000-0000-000000000002',
                    '10000000-0000-0000-0000-000000000001',
                    'INVALID-NEGATIVE-STOCK', 'M', 'Invalid', -1,
                    '2026-03-01T00:00:00Z', '2026-03-01T00:00:00Z'
                )
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
