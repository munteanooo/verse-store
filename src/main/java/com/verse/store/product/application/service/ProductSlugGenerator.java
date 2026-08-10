package com.verse.store.product.application.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.verse.store.product.infrastructure.persistence.ProductRepository;

@Component
public class ProductSlugGenerator {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");

    private final ProductRepository productRepository;

    public ProductSlugGenerator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public String uniqueSlug(String name) {
        String base = normalize(name);
        if (!productRepository.existsBySlug(base)) {
            return base;
        }
        int suffix = 2;
        while (productRepository.existsBySlug(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    String normalize(String name) {
        String withoutDiacritics = DIACRITICS.matcher(
                Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
        String slug = SEPARATORS.matcher(withoutDiacritics.toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = slug.replaceAll("^-|-$", "");
        return slug.isEmpty() ? "product" : slug;
    }
}
