package com.verse.store.product.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.verse.store.product.application.catalog.exception.CatalogProductNotFoundException;
import com.verse.store.product.application.catalog.model.CatalogFilterOption;
import com.verse.store.product.application.catalog.model.CatalogProductDetails;
import com.verse.store.product.application.catalog.model.CatalogProductImage;
import com.verse.store.product.application.catalog.model.CatalogProductSummary;
import com.verse.store.product.application.catalog.model.CatalogProductVariant;
import com.verse.store.product.application.catalog.query.CatalogProductQuery;
import com.verse.store.product.application.catalog.service.ProductCatalogService;
import com.verse.store.product.domain.ProductCategory;

@WebMvcTest(CatalogPageController.class)
@Import(WebPageExceptionHandler.class)
class CatalogPageControllerTests {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProductCatalogService catalogService;

    @BeforeEach
    void filters() {
        when(catalogService.listActiveCategories()).thenReturn(List.of(new CatalogFilterOption("TOPS", "Tops")));
        when(catalogService.listActiveCollections()).thenReturn(List.of("Coastal Lines"));
    }

    @Test
    void rootRedirectsToCatalog() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/catalog"));
    }

    @Test
    void catalogRendersProductsPriceDiscountLabelsImagesAndExactlyTwoDropdowns() throws Exception {
        when(catalogService.listActiveProducts(any())).thenReturn(new PageImpl<>(List.of(summary(true)), PageRequest.of(0, 12), 1));
        mockMvc.perform(get("/catalog")).andExpect(status().isOk()).andExpect(view().name("catalog/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Catalog Product")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("€90.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("−10%")))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        occurrences(result.getResponse().getContentAsString(), "<select ")).isEqualTo(2))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("alt=\"Catalog Product in Sand\"")));
    }

    @Test
    void catalogShowsSoldOutLabel() throws Exception {
        when(catalogService.listActiveProducts(any())).thenReturn(new PageImpl<>(List.of(summary(false))));
        mockMvc.perform(get("/catalog")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Sold out")));
    }

    @Test
    void filtersArePassedToApplicationService() throws Exception {
        when(catalogService.listActiveProducts(any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/catalog").param("category", "TOPS").param("collection", "Coastal Lines")).andExpect(status().isOk());
        ArgumentCaptor<CatalogProductQuery> captor = ArgumentCaptor.forClass(CatalogProductQuery.class);
        verify(catalogService).listActiveProducts(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().category()).isEqualTo(ProductCategory.TOPS);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().collection()).isEqualTo("Coastal Lines");
    }

    @Test
    void emptyCatalogHasUsefulState() throws Exception {
        when(catalogService.listActiveProducts(any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/catalog")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Nothing here just yet")));
    }

    @Test
    void activeProductDetailRendersOptionsAndGallery() throws Exception {
        when(catalogService.getActiveProductBySlug("catalog-product")).thenReturn(details());
        mockMvc.perform(get("/catalog/products/catalog-product")).andExpect(status().isOk()).andExpect(view().name("catalog/detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Available options")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("alt=\"Catalog Product in Sand\"")));
    }

    @Test
    void missingProductRendersGenericHtml404() throws Exception {
        when(catalogService.getActiveProductBySlug("missing")).thenThrow(new CatalogProductNotFoundException());
        mockMvc.perform(get("/catalog/products/missing")).andExpect(status().isNotFound()).andExpect(view().name("error/404"))
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void unexpectedPageFailureRendersHtml500() throws Exception {
        when(catalogService.getActiveProductBySlug("broken")).thenThrow(new IllegalStateException("internal detail"));
        mockMvc.perform(get("/catalog/products/broken")).andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("internal detail"))));
    }

    private CatalogProductSummary summary(boolean available) {
        return new CatalogProductSummary(UUID.randomUUID(), "Catalog Product", "catalog-product", "Verse", "Coastal Lines", ProductCategory.TOPS,
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"), "https://images.example.com/product.jpg", "Catalog Product in Sand", available, available ? 5 : 0);
    }

    private CatalogProductDetails details() {
        return new CatalogProductDetails(UUID.randomUUID(), "Catalog Product", "catalog-product", "A considered product.", "Verse", "Coastal Lines", ProductCategory.TOPS,
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"), "https://images.example.com/product.jpg", "Catalog Product in Sand", true, 5,
                List.of(new CatalogProductVariant(UUID.randomUUID(), "M", "Sand", "#D8C3A5", 5, true)),
                List.of(new CatalogProductImage(UUID.randomUUID(), "https://images.example.com/product.jpg", "Catalog Product in Sand", 0, true)));
    }

    private static int occurrences(String value, String token) {
        return (value.length() - value.replace(token, "").length()) / token.length();
    }
}
