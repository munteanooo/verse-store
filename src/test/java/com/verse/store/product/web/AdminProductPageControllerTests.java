package com.verse.store.product.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.verse.store.product.application.exception.ProductNotFoundException;
import com.verse.store.product.application.model.ProductImageResult;
import com.verse.store.product.application.model.ProductResult;
import com.verse.store.product.application.model.ProductVariantResult;
import com.verse.store.product.application.service.ProductApplicationService;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;

@WebMvcTest(value = AdminProductPageController.class, excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(WebPageExceptionHandler.class)
class AdminProductPageControllerTests {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProductApplicationService productService;

    @Test
    void adminListRendersFiltersActionsAndProduct() throws Exception {
        when(productService.listProductsForAdmin(any())).thenReturn(new PageImpl<>(List.of(product())));
        mockMvc.perform(get("/admin/products")).andExpect(status().isOk()).andExpect(view().name("admin/product-list"))
                .andExpect(model().attributeExists("statuses", "categories"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Product Control Plane")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Publish")));
    }

    @Test
    void newProductFormRendersAtLeastOneVariantAndImage() throws Exception {
        mockMvc.perform(get("/admin/products/new")).andExpect(status().isOk()).andExpect(view().name("admin/product-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("variant-row")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("image-row")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-image-drop-zone")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose files")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Add by URL")));
    }

    @Test
    void editProductFormUsesApplicationResult() throws Exception {
        ProductResult product = product();
        when(productService.getProductForAdmin(product.id())).thenReturn(product);
        mockMvc.perform(get("/admin/products/{id}/edit", product.id())).andExpect(status().isOk()).andExpect(view().name("admin/product-form"))
                .andExpect(model().attribute("editing", true)).andExpect(content().string(org.hamcrest.Matchers.containsString(product.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VRS-001")));
    }

    @Test
    void missingEditProductRendersHtml404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getProductForAdmin(id)).thenThrow(new ProductNotFoundException(id));
        mockMvc.perform(get("/admin/products/{id}/edit", id)).andExpect(status().isNotFound()).andExpect(view().name("error/404"));
    }

    private ProductResult product() {
        UUID id = UUID.randomUUID(); Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ProductResult(id, "Admin Product", "admin-product", "Description", "Verse", "Control", ProductCategory.TOPS,
                new BigDecimal("80.00"), BigDecimal.ZERO, new BigDecimal("80.00"), ProductStatus.DRAFT, 4,
                List.of(new ProductVariantResult(UUID.randomUUID(), "VRS-001", "M", "Ink", "#111111", 4, now, now)),
                List.of(new ProductImageResult(UUID.randomUUID(), "https://images.example.com/admin.jpg", "Admin Product", 0, true, now)), now, now);
    }
}
