package com.verse.store.shared.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Set<String> APPLICATION_ROLES = Set.of("CUSTOMER", "ADMIN");

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return convertClaims(jwt.getClaims());
    }

    public Collection<GrantedAuthority> convertClaims(Map<String, Object> claims) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Object realmAccess = claims.get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> access)) {
            return authorities;
        }
        Object roles = access.get("roles");
        if (!(roles instanceof Collection<?> roleNames)) {
            return authorities;
        }
        roleNames.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(APPLICATION_ROLES::contains)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);
        return authorities;
    }
}
