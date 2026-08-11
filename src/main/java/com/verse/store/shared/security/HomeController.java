package com.verse.store.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final String introduction;

    public HomeController(@Value("${app.storefront.introduction}") String introduction) {
        this.introduction = introduction;
    }

    @GetMapping("/")
    String home(Authentication authentication, Model model) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        if (!authenticated) {
            return "storefront/landing";
        }
        model.addAttribute("introduction", introduction);
        return "storefront/home";
    }
}
