package com.verse.store.product.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.verse.store.product.api.catalog.mapper.CatalogProductApiMapper;
import com.verse.store.product.application.catalog.exception.CatalogProductNotFoundException;
import com.verse.store.product.application.catalog.model.CatalogFilterOption;
import com.verse.store.product.application.catalog.model.CatalogProductDetails;
import com.verse.store.product.application.catalog.model.CatalogProductImage;
import com.verse.store.product.application.catalog.model.CatalogProductSummary;
import com.verse.store.product.application.catalog.model.CatalogProductVariant;
import com.verse.store.product.application.catalog.query.CatalogProductQuery;
import com.verse.store.product.application.catalog.service.ProductCatalogService;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.shared.api.GlobalExceptionHandler;

@WebMvcTest(value = CatalogProductController.class, excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({CatalogProductApiMapper.class, GlobalExceptionHandler.class})
class CatalogProductControllerTests {

    private static final UUID PRODUCT_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductCatalogService catalogService;

    @Test
    void listsProductsWithDefaults() throws Exception {
        when(catalogService.listActiveProducts(any(CatalogProductQuery.class)))
                .thenReturn(new PageImpl<>(List.of(summary()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("catalog-product"));

        ArgumentCaptor<CatalogProductQuery> query = ArgumentCaptor.forClass(CatalogProductQuery.class);
        verify(catalogService).listActiveProducts(query.capture());
        assertThat(query.getValue().page()).isZero();
        assertThat(query.getValue().size()).isEqualTo(20);
    }

    @Test
    void exposesExplicitPaginationMetadata() throws Exception {
        when(catalogService.listActiveProducts(any(CatalogProductQuery.class)))
                .thenReturn(new PageImpl<>(List.of(summary()), PageRequest.of(1, 2), 5));

        mockMvc.perform(get("/api/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void acceptsValidCategory() throws Exception {
        when(catalogService.listActiveProducts(any(CatalogProductQuery.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/catalog/products").param("category", "TOPS"))
                .andExpect(status().isOk());

        ArgumentCaptor<CatalogProductQuery> query = ArgumentCaptor.forClass(CatalogProductQuery.class);
        verify(catalogService).listActiveProducts(query.capture());
        assertThat(query.getValue().category()).isEqualTo(ProductCategory.TOPS);
    }

    @Test
    void acceptsCollectionFilter() throws Exception {
        when(catalogService.listActiveProducts(any(CatalogProductQuery.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/catalog/products").param("collection", "Coastal Lines"))
                .andExpect(status().isOk());

        ArgumentCaptor<CatalogProductQuery> query = ArgumentCaptor.forClass(CatalogProductQuery.class);
        verify(catalogService).listActiveProducts(query.capture());
        assertThat(query.getValue().collection()).isEqualTo("Coastal Lines");
    }

    @Test
    void invalidCategoryReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/catalog/products").param("category", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_PARAMETER_INVALID"));
    }

    @Test
    void negativePageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/catalog/products").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_PARAMETER_INVALID"));
    }

    @Test
    void oversizedPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/catalog/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_PARAMETER_INVALID"));
    }

    @Test
    void returnsActiveProductDetails() throws Exception {
        when(catalogService.getActiveProductBySlug("catalog-product")).thenReturn(details());

        mockMvc.perform(get("/api/catalog/products/catalog-product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("catalog-product"))
                .andExpect(jsonPath("$.variants[0].available").value(true))
                .andExpect(jsonPath("$.variants[0].sku").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void absentOrNonActiveProductReturnsGenericNotFound() throws Exception {
        when(catalogService.getActiveProductBySlug("hidden-product"))
                .thenThrow(new CatalogProductNotFoundException());

        mockMvc.perform(get("/api/catalog/products/hidden-product"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATALOG_PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Catalog product not found"));
    }

    @Test
    void listsCategoryOptions() throws Exception {
        when(catalogService.listActiveCategories()).thenReturn(List.of(
                new CatalogFilterOption("TOPS", "Tops"),
                new CatalogFilterOption("OUTERWEAR", "Outerwear")));

        mockMvc.perform(get("/api/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("TOPS"))
                .andExpect(jsonPath("$[0].label").value("Tops"));
    }

    @Test
    void listsActiveCollections() throws Exception {
        when(catalogService.listActiveCollections())
                .thenReturn(List.of("Coastal Lines", "Quiet Terrain"));

        mockMvc.perform(get("/api/catalog/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Coastal Lines"))
                .andExpect(jsonPath("$[1]").value("Quiet Terrain"));
    }

    private static CatalogProductSummary summary() {
        return new CatalogProductSummary(
                PRODUCT_ID, "Catalog Product", "catalog-product", "Verse", "Coastal Lines",
                ProductCategory.TOPS, new BigDecimal("100.00"), new BigDecimal("10.00"),
                new BigDecimal("90.00"), "https://images.example.com/catalog.jpg",
                "Catalog Product", true, 5);
    }

    private static CatalogProductDetails details() {
        return new CatalogProductDetails(
                PRODUCT_ID, "Catalog Product", "catalog-product", "Description", "Verse",
                "Coastal Lines", ProductCategory.TOPS, new BigDecimal("100.00"),
                new BigDecimal("10.00"), new BigDecimal("90.00"),
                "https://images.example.com/catalog.jpg", "Catalog Product", true, 5,
                List.of(new CatalogProductVariant(
                        UUID.fromString("91000000-0000-0000-0000-000000000001"),
                        "M", "Sand", "#D8C3A5", 5, true)),
                List.of(new CatalogProductImage(
                        UUID.fromString("92000000-0000-0000-0000-000000000001"),
                        "https://images.example.com/catalog.jpg", "Catalog Product", 0, true)));
    }
}
