package com.verse.store.shared.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

public final class PublicOidcLogoutSuccessHandler implements LogoutSuccessHandler {

    private final String endSessionEndpoint;
    private final String clientId;
    private final String postLogoutRedirectUri;

    public PublicOidcLogoutSuccessHandler(
            String keycloakPublicUrl,
            String realm,
            String clientId,
            String postLogoutRedirectUri) {
        this.endSessionEndpoint = keycloakPublicUrl + "/realms/" + realm
                + "/protocol/openid-connect/logout";
        this.clientId = clientId;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
            String logoutUri = UriComponentsBuilder.fromUriString(endSessionEndpoint)
                    .queryParam("id_token_hint", user.getIdToken().getTokenValue())
                    .queryParam("client_id", clientId)
                    .queryParam("post_logout_redirect_uri", postLogoutRedirectUri)
                    .build()
                    .encode()
                    .toUriString();
            response.sendRedirect(logoutUri);
            return;
        }
        response.sendRedirect(postLogoutRedirectUri);
    }
}
