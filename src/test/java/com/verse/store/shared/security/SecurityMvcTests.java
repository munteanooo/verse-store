package com.verse.store.shared.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.verse.store.product.api.admin.ProductImageUploadController;
import com.verse.store.product.api.admin.mapper.AdminProductApiMapper;
import com.verse.store.product.api.catalog.CatalogProductController;
import com.verse.store.product.api.catalog.mapper.CatalogProductApiMapper;
import com.verse.store.product.application.catalog.service.ProductCatalogService;
import com.verse.store.product.application.service.ProductApplicationService;
import com.verse.store.product.application.image.ProductImageUploadService;
import com.verse.store.product.application.image.ProductImageUploadResult;
import com.verse.store.product.web.AdminProductPageController;
import com.verse.store.product.web.CatalogPageController;

@WebMvcTest(value = {CatalogPageController.class, AdminProductPageController.class,
        AdminProductController.class, ProductImageUploadController.class,
        ProfileController.class, RegistrationController.class,
        HomeController.class, CatalogProductController.class}, excludeAutoConfiguration = {
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
    @MockitoBean CatalogProductApiMapper catalogProductApiMapper;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean ProductImageUploadService imageUploadService;

    @BeforeEach
    void setUpPages() {
        when(catalogService.listActiveProducts(any())).thenReturn(Page.empty());
        when(catalogService.listActiveCategories()).thenReturn(List.of());
        when(catalogService.listActiveCollections()).thenReturn(List.of());
        when(productService.listProductsForAdmin(any())).thenReturn(Page.empty());
    }

    @Test
    void adminCanUploadProductImageWithCsrf() throws Exception {
        when(imageUploadService.upload(any())).thenReturn(new ProductImageUploadResult(
                "products/10000000-0000-0000-0000-000000000001.jpg",
                "/media/products/10000000-0000-0000-0000-000000000001.jpg"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(
                        "/api/admin/product-images")
                .file(new org.springframework.mock.web.MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", new byte[]{1}))
                .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(
                        "/media/products/10000000-0000-0000-0000-000000000001.jpg"));
    }

    @Test
    void productImageUploadEnforcesAuthenticationAdminAndCsrf() throws Exception {
        mockMvc.perform(imageUploadRequest().with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(imageUploadRequest().with(user("customer").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(imageUploadRequest().with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            imageUploadRequest() {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(
                        "/api/admin/product-images")
                .file(new org.springframework.mock.web.MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", new byte[]{1}));
    }

    @Test
    void anonymousLandingOffersOnlyAuthenticationChoices() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login")))
                .andExpect(content().string(containsString("Create account")));
    }

    @Test
    void authenticatedCustomerSeesHomeAndCatalogNavigation() throws Exception {
        mockMvc.perform(get("/").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Essential forms.")))
                .andExpect(content().string(containsString("Catalog")));
    }

    @Test
    void anonymousCatalogRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/catalog"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/verse-store"));
    }

    @Test
    void oidcUserWithoutCustomerRoleCanViewCatalogAndHasSimplifiedHeader() throws Exception {
        mockMvc.perform(get("/catalog").with(oidcLogin()
                        .idToken(token -> token.claim("name", "New Member"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Login<"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Register<"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Logout<"))))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        org.springframework.util.StringUtils.countOccurrencesOf(
                                result.getResponse().getContentAsString(), "href=\"/\""))
                        .isEqualTo(1))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Home</a>"))))
                .andExpect(content().string(containsString("href=\"/profile\">New Member</a>")));
    }

    @Test
    void anonymousCatalogApiReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/catalog/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authenticatedOidcUserWithoutCustomerRoleCanUseCatalogApi() throws Exception {
        mockMvc.perform(get("/api/catalog/products").with(oidcLogin()))
                .andExpect(status().isOk());
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
                                .claim("name", "Considered Customer")
                                .claim("preferred_username", "customer")
                                .claim("email", "customer@example.test")
                                .claim("email_verified", true))
                        .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Considered Customer")))
                .andExpect(content().string(containsString("customer@example.test")))
                .andExpect(content().string(containsString("CUSTOMER")))
                .andExpect(content().string(containsString("Edit personal information")))
                .andExpect(content().string(containsString("Change password")))
                .andExpect(content().string(containsString("Delete / deactivate account")))
                .andExpect(content().string(containsString("Logout")));
    }

    @Test
    void oidcUserWithoutCustomerEmailOrUsernameCanViewProfile() throws Exception {
        mockMvc.perform(get("/profile").with(oidcLogin()
                        .idToken(token -> token
                                .claims(claims -> {
                                    claims.remove("preferred_username");
                                    claims.remove("email");
                                })
                                .claim("name", "Name Only"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Name Only")));
    }

    @Test
    void oidcUserWithOnlySubjectUsesFriendlyProfileFallback() throws Exception {
        mockMvc.perform(get("/profile").with(oidcLogin()
                        .idToken(token -> token.subject("sensitive-subject-123").claims(claims -> {
                            claims.remove("preferred_username");
                            claims.remove("email");
                            claims.remove("name");
                        }))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Verse member")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("sensitive-subject-123"))));
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
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authenticatedUserWithoutAdminCannotUseAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/products").with(oidcLogin()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
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
    void customerCannotPublishArchiveOrDeleteProducts() throws Exception {
        String product = "/api/admin/products/70000000-0000-0000-0000-000000000001";

        mockMvc.perform(patch(product + "/publish").with(user("customer").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(product + "/archive").with(user("customer").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(product).with(user("customer").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lifecycleMutationsWithoutCsrfAreRejected() throws Exception {
        String product = "/api/admin/products/70000000-0000-0000-0000-000000000001";

        mockMvc.perform(patch(product + "/publish").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(product + "/archive").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(product).with(user("admin").roles("ADMIN")))
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
                        "http://localhost:8180/realms/verse/protocol/openid-connect/logout?")))
                .andExpect(header().string("Location", containsString("id_token_hint=")))
                .andExpect(header().string("Location", containsString("client_id=verse-store")))
                .andExpect(header().string("Location", containsString("post_logout_redirect_uri=")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.not(containsString("keycloak:8080"))));
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
