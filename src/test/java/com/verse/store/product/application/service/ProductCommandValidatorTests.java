package com.verse.store.product.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.verse.store.product.application.command.CreateProductCommand;
import com.verse.store.product.application.command.CreateProductImageCommand;
import com.verse.store.product.application.command.CreateProductVariantCommand;
import com.verse.store.product.application.exception.ProductValidationException;
import com.verse.store.product.domain.ProductCategory;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class ProductCommandValidatorTests {

    private static ValidatorFactory validatorFactory;
    private static ProductCommandValidator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = new ProductCommandValidator(
                validatorFactory.getValidator(), new org.springframework.mock.env.MockEnvironment());
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void rejectsDuplicateSkusAndSizeColorCombinations() {
        List<CreateProductVariantCommand> variants = List.of(
                new CreateProductVariantCommand("DUP-SKU", "M", "Sand", null, 1),
                new CreateProductVariantCommand(" dup-sku ", "m", " sand ", null, 2));

        assertThatThrownBy(() -> validator.validate(command(variants, onePrimaryImage())))
                .isInstanceOf(ProductValidationException.class)
                .hasMessageContaining("duplicate SKU")
                .hasMessageContaining("duplicate size and color");
    }

    @Test
    void rejectsMoreThanOnePrimaryImage() {
        List<CreateProductImageCommand> images = List.of(
                new CreateProductImageCommand("https://images.example.com/one.jpg", null, 0, true),
                new CreateProductImageCommand("https://images.example.com/two.jpg", null, 1, true));

        assertThatThrownBy(() -> validator.validate(command(oneVariant(), images)))
                .isInstanceOf(ProductValidationException.class)
                .hasMessageContaining("at most one primary image");
    }

    private static CreateProductCommand command(
            List<CreateProductVariantCommand> variants,
            List<CreateProductImageCommand> images) {
        return new CreateProductCommand(
                "Validated Product", null, "Verse", "Validated Collection",
                ProductCategory.TOPS, new BigDecimal("10.00"), BigDecimal.ZERO,
                variants, images);
    }

    private static List<CreateProductVariantCommand> oneVariant() {
        return List.of(new CreateProductVariantCommand("VALID-SKU", "M", "Sand", null, 1));
    }

    private static List<CreateProductImageCommand> onePrimaryImage() {
        return List.of(new CreateProductImageCommand(
                "https://images.example.com/one.jpg", null, 0, true));
    }
}
