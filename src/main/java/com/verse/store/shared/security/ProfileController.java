package com.verse.store.shared.security;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final String accountConsoleUrl;

    public ProfileController(
            @Value("${app.security.keycloak-public-url}") String keycloakBaseUrl,
            @Value("${app.security.realm}") String realm) {
        this.accountConsoleUrl = keycloakBaseUrl + "/realms/" + realm + "/account";
    }

    @GetMapping("/profile")
    String profile(@AuthenticationPrincipal OidcUser user, Model model) {
        Set<String> relevantRoles = Set.of("CUSTOMER", "ADMIN");
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .filter(relevantRoles::contains)
                .sorted()
                .toList();
        String username = claim(user, "preferred_username");
        String name = claim(user, "name");
        String displayName = name != null ? name : username;
        if (displayName == null) {
            displayName = "Verse member";
        }
        model.addAttribute("profile", new ProfileViewModel(
                displayName, username, claim(user, "email"), roles));
        model.addAttribute("accountConsoleUrl", accountConsoleUrl + "/");
        return "profile/index";
    }

    private String claim(OidcUser user, String name) {
        Object value = user.getClaim(name);
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
