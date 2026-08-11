package com.verse.store.shared.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.verse.store.product.api.admin.AdminProductController;
import com.verse.store.product.api.admin.mapper.AdminProductApiMapper;
import com.verse.store.product.application.catalog.service.ProductCatalogService;
import com.verse.store.product.application.service.ProductApplicationService;
import com.verse.store.product.web.AdminProductPageController;
import com.verse.store.product.web.CatalogPageController;

@WebMvcTest(value = {CatalogPageController.class, AdminProductPageController.class,
        AdminProductController.class, ProfileController.class, RegistrationController.class}, excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
@Import({SecurityConfig.class, KeycloakRealmRoleConverter.class,
        SecurityNavigationAdvice.class, SecurityMvcTests.OAuthClientTestConfig.class})
class SecurityMvcTests {

    @Autowired MockMvc mockMvc;
    @Autowired ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean ProductCatalogService catalogService;
    @MockitoBean ProductApplicationService productService;
    @MockitoBean AdminProductApiMapper adminProductApiMapper;
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach
    void setUpPages() {
        when(catalogService.listActiveProducts(any())).thenReturn(Page.empty());
        when(catalogService.listActiveCategories()).thenReturn(List.of());
        when(catalogService.listActiveCollections()).thenReturn(List.of());
        when(productService.listProductsForAdmin(any())).thenReturn(Page.empty());
    }

    @Test
    void anonymousCatalogIsPublic() throws Exception {
        mockMvc.perform(get("/catalog")).andExpect(status().isOk());
    }

    @Test
    void registerStartsSpringSecurityAuthorizationFlow() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/verse-store?prompt=create"));
    }

    @Test
    void registrationPromptIsIncludedWithSpringGeneratedStateAndNonce() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/verse-store").param("prompt", "create"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString(
                        "http://localhost:8180/realms/verse/protocol/openid-connect/auth?")))
                .andExpect(header().string("Location", containsString("prompt=create")))
                .andExpect(header().string("Location", containsString("state=")))
                .andExpect(header().string("Location", containsString("nonce=")))
                .andExpect(header().string("Location", containsString(
                        "redirect_uri=http://localhost/login/oauth2/code/verse-store")));
    }

    @Test
    void loginPageStillOffersNormalOidcLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/oauth2/authorization/verse-store")));
    }

    @Test
    void missingStaticResourceReturnsNotFound() throws Exception {
        mockMvc.perform(get("/css/missing-resource-for-security-test.css"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("RESOURCE_NOT_FOUND")));
    }

    @Test
    void authenticatedUserIsNotSentThroughRegistrationAgain() throws Exception {
        mockMvc.perform(get("/register").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    void anonymousProfileRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/verse-store"));
    }

    @Test
    void customerCanViewProfile() throws Exception {
        mockMvc.perform(get("/profile").with(oidcLogin()
                        .idToken(token -> token
                                .claim("preferred_username", "customer")
                                .claim("email", "customer@example.test")
                                .claim("email_verified", true))
                        .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("customer@example.test")));
    }

    @Test
    void anonymousAdminUiRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/products")).andExpect(status().isFound());
    }

    @Test
    void customerCannotViewAdminUi() throws Exception {
        mockMvc.perform(get("/admin/products").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewControlPlane() throws Exception {
        mockMvc.perform(get("/admin/products").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousAdminApiReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotUseAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/products").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPassesAdminApiAuthorization() throws Exception {
        mockMvc.perform(get("/api/admin/products").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminMutationWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminMutationWithCsrfPassesSecurity() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutUsesKeycloakEndSessionEndpoint() throws Exception {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("verse-store");
        mockMvc.perform(post("/logout").with(oidcLogin().clientRegistration(registration)).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(
                        "http://localhost:8180/realms/verse/protocol/openid-connect/logout?")));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class OAuthClientTestConfig {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration registration = ClientRegistration.withRegistrationId("verse-store")
                    .clientId("verse-store")
                    .clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .authorizationUri("http://localhost:8180/realms/verse/protocol/openid-connect/auth")
                    .tokenUri("http://localhost:8180/realms/verse/protocol/openid-connect/token")
                    .jwkSetUri("http://localhost:8180/realms/verse/protocol/openid-connect/certs")
                    .userInfoUri("http://localhost:8180/realms/verse/protocol/openid-connect/userinfo")
                    .userNameAttributeName("preferred_username")
                    .providerConfigurationMetadata(Map.of(
                            "end_session_endpoint",
                            "http://localhost:8180/realms/verse/protocol/openid-connect/logout"))
                    .clientName("Verse Store")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }
    }
}
