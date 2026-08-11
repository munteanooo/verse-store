package com.verse.store.shared.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class SecurityNavigationAdvice {

    @ModelAttribute
    void securityNavigation(Authentication authentication, Model model) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("authenticated", authenticated);
        String displayName = authentication == null ? null : authentication.getName();
        if (authenticated && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser user) {
            String name = user.getClaimAsString("name");
            displayName = name == null || name.isBlank() ? user.getPreferredUsername() : name;
            if (displayName == null || displayName.isBlank()) {
                displayName = "Verse member";
            }
        }
        model.addAttribute("currentUsername", authenticated ? displayName : null);
        model.addAttribute("currentUserAdmin", authenticated && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }
}
