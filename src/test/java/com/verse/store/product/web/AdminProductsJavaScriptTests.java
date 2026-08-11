package com.verse.store.product.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AdminProductsJavaScriptTests {

    @Test
    void usesStatusSpecificDeleteConfirmationsAndPreventsDuplicateRequests() throws IOException {
        String script;
        try (var input = getClass().getResourceAsStream("/static/js/admin-products.js")) {
            if (input == null) {
                throw new IllegalStateException("admin-products.js is missing");
            }
            script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(script)
                .contains("Delete this product permanently?")
                .contains("This product is currently published. Delete it permanently and remove it from the catalog?")
                .contains("if (row.dataset.busy === 'true') return;")
                .contains("row.dataset.busy = 'true'")
                .contains("actionButton.disabled = true");
    }
}
