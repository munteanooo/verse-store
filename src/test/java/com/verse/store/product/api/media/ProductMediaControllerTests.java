package com.verse.store.product.api.media;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.verse.store.product.application.image.ProductImageStorage;

@WebMvcTest(value = ProductMediaController.class, excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ProductMediaControllerTests {
    private static final String FILE = "10000000-0000-0000-0000-000000000001.png";

    @Autowired MockMvc mockMvc;
    @MockitoBean ProductImageStorage storage;

    @Test
    void servesStoredImageWithItsContentType() throws Exception {
        when(storage.load("products/" + FILE))
                .thenReturn(new ProductImageStorage.StoredImage(new byte[]{1, 2, 3}, "image/png"));
        mockMvc.perform(get("/media/products/" + FILE))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }
}
