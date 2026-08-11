package com.verse.store.shared.security;

import java.util.List;

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
            @Value("${verse.security.keycloak-public-base-url}") String keycloakBaseUrl,
            @Value("${verse.security.realm}") String realm) {
        this.accountConsoleUrl = keycloakBaseUrl + "/realms/" + realm + "/account";
    }

    @GetMapping("/profile")
    String profile(@AuthenticationPrincipal OidcUser user, Model model) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .sorted()
                .toList();
        model.addAttribute("profile", user.getClaims());
        model.addAttribute("applicationRoles", roles);
        model.addAttribute("accountConsoleUrl", accountConsoleUrl + "/");
        model.addAttribute("changePasswordUrl", accountConsoleUrl + "/#/security/signingin");
        model.addAttribute("deleteAccountUrl", accountConsoleUrl + "/#/personal-info");
        return "profile/index";
    }
}
