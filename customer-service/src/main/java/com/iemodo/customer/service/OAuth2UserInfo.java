package com.iemodo.customer.service;

/**
 * Standardized OAuth2 user info from any provider.
 */
public record OAuth2UserInfo(
        String provider,
        String subject,
        String email,
        String name,
        String avatarUrl
) {}
