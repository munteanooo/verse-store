package com.verse.store.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.verse.store.product.infrastructure.persistence.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductSlugGeneratorTests {

    @Mock
    private ProductRepository productRepository;

    @Test
    void normalizesSlugAndRemovesDiacritics() {
        ProductSlugGenerator generator = new ProductSlugGenerator(productRepository);

        assertThat(generator.uniqueSlug("  Cămașă de Vară!  ")).isEqualTo("camasa-de-vara");
    }

    @Test
    void addsPredictableSuffixForSlugCollision() {
        when(productRepository.existsBySlug("camasa-de-vara")).thenReturn(true);
        when(productRepository.existsBySlug("camasa-de-vara-2")).thenReturn(true);
        ProductSlugGenerator generator = new ProductSlugGenerator(productRepository);

        assertThat(generator.uniqueSlug("Cămașă de Vară")).isEqualTo("camasa-de-vara-3");
    }
}
