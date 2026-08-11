package com.verse.store.product.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.verse.store.product.api.admin.mapper.AdminProductApiMapper;
import com.verse.store.product.application.command.CreateProductCommand;
import com.verse.store.product.application.command.UpdateProductCommand;
import com.verse.store.product.application.exception.DuplicateProductException;
import com.verse.store.product.application.exception.InvalidProductStateException;
import com.verse.store.product.application.exception.ProductNotFoundException;
import com.verse.store.product.application.model.ProductImageResult;
import com.verse.store.product.application.model.ProductResult;
import com.verse.store.product.application.model.ProductVariantResult;
import com.verse.store.product.application.query.ProductAdminQuery;
import com.verse.store.product.application.query.ProductSortDirection;
import com.verse.store.product.application.service.ProductApplicationService;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;
import com.verse.store.shared.api.GlobalExceptionHandler;

@WebMvcTest(value = AdminProductController.class, excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({AdminProductApiMapper.class, GlobalExceptionHandler.class})
class AdminProductControllerTests {

    private static final UUID PRODUCT_ID = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final String PRODUCT_PATH = "/api/admin/products/" + PRODUCT_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductApplicationService productService;

    @Test
    void createValidReturnsCreatedAndLocation() throws Exception {
        when(productService.createProduct(any(CreateProductCommand.class))).thenReturn(product());

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", PRODUCT_PATH))
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createInvalidReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").isArray())
                .andExpect(jsonPath("$.path").value("/api/admin/products"));
    }

    @Test
    void createDuplicateReturnsConflict() throws Exception {
        when(productService.createProduct(any(CreateProductCommand.class)))
                .thenThrow(new DuplicateProductException("database details must not leak"));

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PRODUCT"))
                .andExpect(jsonPath("$.message").value(
                        "A product with the same slug, SKU, or variant option already exists"));
    }

    @Test
    void getExistingReturnsProduct() throws Exception {
        when(productService.getProductForAdmin(PRODUCT_ID)).thenReturn(product());

        mockMvc.perform(get(PRODUCT_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("admin-product"))
                .andExpect(jsonPath("$.finalPrice").value(90.00))
                .andExpect(jsonPath("$.totalStock").value(6));
    }

    @Test
    void getMissingReturnsNotFound() throws Exception {
        when(productService.getProductForAdmin(PRODUCT_ID))
                .thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(get(PRODUCT_PATH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listDefaultsReturnExplicitPageMetadata() throws Exception {
        when(productService.listProductsForAdmin(any(ProductAdminQuery.class)))
                .thenReturn(new PageImpl<>(List.of(product()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void listPassesStatusAndCategoryFilters() throws Exception {
        when(productService.listProductsForAdmin(any(ProductAdminQuery.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/products")
                        .param("status", "ACTIVE")
                        .param("category", "TOPS")
                        .param("sortBy", "name")
                        .param("direction", "ASC"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductAdminQuery> query = ArgumentCaptor.forClass(ProductAdminQuery.class);
        verify(productService).listProductsForAdmin(query.capture());
        assertThat(query.getValue().status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(query.getValue().category()).isEqualTo(ProductCategory.TOPS);
        assertThat(query.getValue().sortBy()).isEqualTo("name");
        assertThat(query.getValue().direction()).isEqualTo(ProductSortDirection.ASC);
    }

    @Test
    void negativePageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/products").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_PARAMETER_INVALID"));
    }

    @Test
    void sizeAboveMaximumReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_PARAMETER_INVALID"));
    }

    @Test
    void updateReturnsUpdatedProductAndUsesPathId() throws Exception {
        when(productService.updateProduct(any(UpdateProductCommand.class))).thenReturn(product());

        mockMvc.perform(put(PRODUCT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()));

        ArgumentCaptor<UpdateProductCommand> command = ArgumentCaptor.forClass(UpdateProductCommand.class);
        verify(productService).updateProduct(command.capture());
        assertThat(command.getValue().id()).isEqualTo(PRODUCT_ID);
        assertThat(command.getValue().variants().getFirst().id())
                .isEqualTo(UUID.fromString("81000000-0000-0000-0000-000000000001"));
        assertThat(command.getValue().images().getFirst().id())
                .isEqualTo(UUID.fromString("82000000-0000-0000-0000-000000000001"));
    }

    @Test
    void createRejectsChildIds() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PRODUCT_VALIDATION_FAILED"));
    }

    @Test
    void publishReturnsUpdatedProduct() throws Exception {
        when(productService.publishProduct(PRODUCT_ID)).thenReturn(product(ProductStatus.ACTIVE));

        mockMvc.perform(patch(PRODUCT_PATH + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void invalidPublishReturnsConflict() throws Exception {
        when(productService.publishProduct(PRODUCT_ID))
                .thenThrow(new InvalidProductStateException("Product needs a primary image"));

        mockMvc.perform(patch(PRODUCT_PATH + "/publish"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_STATE"));
    }

    @Test
    void archiveReturnsUpdatedProduct() throws Exception {
        when(productService.archiveProduct(PRODUCT_ID)).thenReturn(product(ProductStatus.ARCHIVED));

        mockMvc.perform(patch(PRODUCT_PATH + "/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void deleteDraftReturnsNoContent() throws Exception {
        doNothing().when(productService).deleteProduct(PRODUCT_ID);

        mockMvc.perform(delete(PRODUCT_PATH))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void deleteActiveReturnsConflict() throws Exception {
        doThrow(new InvalidProductStateException("Archive this product instead"))
                .when(productService).deleteProduct(PRODUCT_ID);

        mockMvc.perform(delete(PRODUCT_PATH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_STATE"));
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    void invalidEnumReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/products").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_PARAMETER_INVALID"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void unexpectedErrorReturnsSanitizedInternalServerError() throws Exception {
        when(productService.getProductForAdmin(PRODUCT_ID))
                .thenThrow(new RuntimeException("secret stack and database details"));

        mockMvc.perform(get(PRODUCT_PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    private static String validRequest() {
        return """
                {
                  "name": "Admin Product",
                  "description": "Created through the admin API",
                  "brand": "Verse",
                  "collectionName": "Admin Collection",
                  "category": "TOPS",
                  "basePrice": 100.00,
                  "discountPercentage": 10.00,
                  "variants": [{
                    "sku": "ADMIN-SKU-1",
                    "size": "M",
                    "colorName": "Sand",
                    "colorHex": "#D8C3A5",
                    "stockQuantity": 6
                  }],
                  "images": [{
                    "url": "https://images.example.com/admin-product.jpg",
                    "altText": "Admin Product",
                    "displayOrder": 0,
                    "primaryImage": true
                  }]
                }
                """;
    }

    private static String invalidRequest() {
        return """
                {
                  "name": "",
                  "brand": "Verse",
                  "collectionName": "Admin Collection",
                  "category": "TOPS",
                  "basePrice": 100.00,
                  "discountPercentage": 0,
                  "variants": [],
                  "images": []
                }
                """;
    }

    private static String validUpdateRequest() {
        return validRequest()
                .replace("\"sku\": \"ADMIN-SKU-1\"", """
                        "id": "81000000-0000-0000-0000-000000000001",
                            "sku": "ADMIN-SKU-1"
                        """)
                .replace("\"url\": \"https://images.example.com/admin-product.jpg\"", """
                        "id": "82000000-0000-0000-0000-000000000001",
                            "url": "https://images.example.com/admin-product.jpg"
                        """);
    }

    private static ProductResult product() {
        return product(ProductStatus.DRAFT);
    }

    private static ProductResult product(ProductStatus status) {
        Instant timestamp = Instant.parse("2026-08-11T08:00:00Z");
        return new ProductResult(
                PRODUCT_ID,
                "Admin Product",
                "admin-product",
                "Created through the admin API",
                "Verse",
                "Admin Collection",
                ProductCategory.TOPS,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("90.00"),
                status,
                6,
                List.of(new ProductVariantResult(
                        UUID.fromString("81000000-0000-0000-0000-000000000001"),
                        "ADMIN-SKU-1", "M", "Sand", "#D8C3A5", 6, timestamp, timestamp)),
                List.of(new ProductImageResult(
                        UUID.fromString("82000000-0000-0000-0000-000000000001"),
                        "https://images.example.com/admin-product.jpg",
                        "Admin Product", 0, true, timestamp)),
                timestamp,
                timestamp);
    }
}
