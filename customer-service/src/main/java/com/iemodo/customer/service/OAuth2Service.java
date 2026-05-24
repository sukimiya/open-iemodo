package com.iemodo.customer.service;

import reactor.core.publisher.Mono;

/**
 * OAuth2 authentication interface.
 *
 * <p>Each provider (Google, Facebook, Apple, WeChat) implements this
 * interface to provide provider-specific authorization URL generation
 * and authorization code exchange.
 */
public interface OAuth2Service {

    String name();

    /**
     * Build the authorization URL to redirect the user's browser to.
     *
     * @param state       CSRF token (verified on callback)
     * @param redirectUri the callback URL at our server
     * @return the full authorization URL
     */
    String getAuthorizationUrl(String state, String redirectUri);

    /**
     * Exchange an authorization code for user identity information.
     *
     * @param code        the authorization code from the OAuth2 provider
     * @param redirectUri the redirect URI used in the initial request
     * @return the user info extracted from the provider (subject, email, name, avatar)
     */
    Mono<OAuth2UserInfo> exchangeCodeForUser(String code, String redirectUri);
}
