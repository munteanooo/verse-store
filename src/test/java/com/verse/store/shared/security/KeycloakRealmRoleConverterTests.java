package com.verse.store.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTests {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void mapsOnlyApplicationRealmRoles() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "customer", "realm_access",
                        Map.of("roles", List.of("CUSTOMER", "ADMIN", "offline_access"))));

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    void browserClaimsCannotCreateRolesWithoutRealmAccess() {
        assertThat(converter.convertClaims(Map.of("roles", List.of("ADMIN")))).isEmpty();
    }
}
