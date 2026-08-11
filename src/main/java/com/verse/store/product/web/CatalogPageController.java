package com.verse.store.product.web;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.verse.store.product.application.catalog.model.CatalogProductSummary;
import com.verse.store.product.application.catalog.query.CatalogProductQuery;
import com.verse.store.product.application.catalog.service.ProductCatalogService;
import com.verse.store.product.domain.ProductCategory;

@Controller
public class CatalogPageController {

    private final ProductCatalogService catalogService;

    public CatalogPageController(ProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/catalog";
    }

    @GetMapping("/catalog")
    public String catalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String collection,
            Model model) {
        Page<CatalogProductSummary> products = catalogService.listActiveProducts(
                new CatalogProductQuery(page, size, category, collection));
        model.addAttribute("products", products);
        model.addAttribute("categories", catalogService.listActiveCategories());
        model.addAttribute("collections", catalogService.listActiveCollections());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedCollection", collection);
        return "catalog/index";
    }

    @GetMapping("/catalog/products/{slug}")
    public String product(@PathVariable String slug, Model model) {
        model.addAttribute("product", catalogService.getActiveProductBySlug(slug));
        return "catalog/detail";
    }
}
