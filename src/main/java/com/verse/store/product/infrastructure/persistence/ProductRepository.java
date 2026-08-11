package com.verse.store.product.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategory(ProductCategory category, Pageable pageable);

    Page<Product> findByStatusAndCategory(
            ProductStatus status, ProductCategory category, Pageable pageable);

    @Query("""
            select distinct product.collectionName
            from Product product
            where product.status = com.verse.store.product.domain.ProductStatus.ACTIVE
            order by product.collectionName
            """)
    List<String> findDistinctActiveCollectionNames();

    @Query(value = """
            select product
            from Product product
            where product.status = com.verse.store.product.domain.ProductStatus.ACTIVE
              and (:category is null or product.category = :category)
              and (:collectionName is null or product.collectionName = :collectionName)
            """, countQuery = """
            select count(product)
            from Product product
            where product.status = com.verse.store.product.domain.ProductStatus.ACTIVE
              and (:category is null or product.category = :category)
              and (:collectionName is null or product.collectionName = :collectionName)
            """)
    Page<Product> findActiveCatalogProducts(
            @Param("category") ProductCategory category,
            @Param("collectionName") String collectionName,
            Pageable pageable);

    @Query("""
            select product
            from Product product
            where product.status = com.verse.store.product.domain.ProductStatus.ACTIVE
              and product.slug = :slug
            """)
    Optional<Product> findActiveCatalogProductBySlug(@Param("slug") String slug);

    @Query("""
            select distinct product
            from Product product
            left join fetch product.variants
            where product.id in :ids
            """)
    List<Product> loadCatalogVariants(@Param("ids") List<UUID> ids);

    @Query("""
            select distinct product
            from Product product
            left join fetch product.images
            where product.id in :ids
            """)
    List<Product> loadCatalogImages(@Param("ids") List<UUID> ids);
}
