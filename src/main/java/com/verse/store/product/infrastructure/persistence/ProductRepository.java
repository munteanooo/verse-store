package com.verse.store.product.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndCategory(
            ProductStatus status, ProductCategory category, Pageable pageable);

    @Query("""
            select distinct product.collectionName
            from Product product
            where product.status = com.verse.store.product.domain.ProductStatus.ACTIVE
            order by product.collectionName
            """)
    List<String> findDistinctActiveCollectionNames();
}
