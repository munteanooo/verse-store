package com.verse.store.shared.security;

import java.util.LinkedHashSet;
import java.time.Instant;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
            LogoutSuccessHandler oidcLogoutSuccessHandler,
            ClientRegistrationRepository registrations,
            ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint(
                "/oauth2/authorization/verse-store");
        AuthenticationEntryPoint apiUnauthorized = (request, response, exception) ->
                writeSecurityError(objectMapper, response, request.getRequestURI(),
                        HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        AccessDeniedHandler apiForbidden = (request, response, exception) ->
                writeSecurityError(objectMapper, response, request.getRequestURI(),
                        HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access is denied");
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/css/**", "/js/**",
                                "/images/**", "/actuator/health/**", "/error", "/login/**",
                                "/register", "/oauth2/**", "/actuator/prometheus")
                        .permitAll()
                        .requestMatchers("/catalog/**", "/api/catalog/**", "/profile/**")
                        .authenticated()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                apiUnauthorized,
                                request -> request.getRequestURI().startsWith("/api/"))
                        .defaultAccessDeniedHandlerFor(
                                apiForbidden,
                                request -> request.getRequestURI().startsWith("/api/"))
                        .defaultAuthenticationEntryPointFor(login, AnyRequestMatcher.INSTANCE))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(
                                new RegistrationAuthorizationRequestResolver(registrations)))
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService)))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .logout(logout -> logout.logoutSuccessHandler(oidcLogoutSuccessHandler))
                .csrf(Customizer.withDefaults());
        return http.build();
    }

    private void writeSecurityError(
            ObjectMapper objectMapper,
            jakarta.servlet.http.HttpServletResponse response,
            String path,
            HttpStatus status,
            String code,
            String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "code", code,
                "message", message,
                "path", path));
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakRealmRoleConverter roleConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roleConverter);
        return converter;
    }

    @Bean
    JwtDecoderFactory<ClientRegistration> oidcIdTokenDecoderFactory(
            @Value("${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/verse}") String issuer) {
        OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
        factory.setJwtValidatorFactory(registration -> {
            OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer);
            return validator;
        });
        return factory;
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(KeycloakRealmRoleConverter roleConverter) {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser user = delegate.loadUser(request);
            var authorities = new LinkedHashSet<GrantedAuthority>(user.getAuthorities());
            roleConverter.convertClaims(user.getClaims()).stream()
                    .forEach(authorities::add);
            return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo(), "preferred_username");
        };
    }

    @Bean
    LogoutSuccessHandler oidcLogoutSuccessHandler(
            @Value("${app.security.keycloak-public-url}") String keycloakPublicUrl,
            @Value("${app.security.realm}") String realm,
            @Value("${app.security.client-id}") String clientId,
            @Value("${app.security.post-logout-redirect-uri}") String redirectUri) {
        return new PublicOidcLogoutSuccessHandler(
                keycloakPublicUrl, realm, clientId, redirectUri);
    }
}
