package io.github.susimsek.springauthserversamples.dto.admin;

public record AdminServerInfoDTO(
        String issuer,
        String discoveryEndpoint,
        String authorizationEndpoint,
        String tokenEndpoint,
        String introspectionEndpoint,
        String revocationEndpoint,
        String jwksEndpoint,
        String userInfoEndpoint,
        String endSessionEndpoint,
        String sessionTimeout,
        AdminSigningKeySummaryDTO activeSigningKey) {}
