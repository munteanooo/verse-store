package com.verse.store.product.web;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.verse.store.product.application.model.ProductResult;
import com.verse.store.product.application.query.ProductAdminQuery;
import com.verse.store.product.application.query.ProductSortDirection;
import com.verse.store.product.application.service.ProductApplicationService;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;

@Controller
@RequestMapping("/admin/products")
public class AdminProductPageController {

    private final ProductApplicationService productService;

    public AdminProductPageController(ProductApplicationService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String notice,
            Model model) {
        Page<ProductResult> products = productService.listProductsForAdmin(new ProductAdminQuery(
                page, size, status, category, "createdAt", ProductSortDirection.DESC));
        model.addAttribute("products", products);
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("notice", notice);
        return "admin/product-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        formModel(model, null, false);
        return "admin/product-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        formModel(model, productService.getProductForAdmin(id), true);
        return "admin/product-form";
    }

    private void formModel(Model model, ProductResult product, boolean editing) {
        model.addAttribute("product", product);
        model.addAttribute("editing", editing);
        model.addAttribute("categories", Arrays.asList(ProductCategory.values()));
    }
}
