package com.verse.store.shared.security;

import java.util.LinkedHashSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
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
            ClientRegistrationRepository registrations) throws Exception {
        AuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint(
                "/oauth2/authorization/verse-store");
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/catalog/**", "/api/catalog/**", "/css/**", "/js/**",
                                "/images/**", "/actuator/health/**", "/error", "/login/**",
                                "/register", "/oauth2/**")
                        .permitAll()
                        .requestMatchers("/profile/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
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
            ClientRegistrationRepository registrations,
            @Value("${verse.security.post-logout-redirect-uri}") String redirectUri) {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(registrations);
        handler.setPostLogoutRedirectUri(redirectUri);
        return handler;
    }
}
