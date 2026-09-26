package io.github.susimsek.springauthserversamples.security;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/** Application-specific security settings stored in a registered client's settings map. */
public final class ClientSecuritySettings {

    public static final String REQUIRE_DPOP_PROOF = "settings.client.require-dpop-proof";
    public static final String REQUIRE_DPOP_JKT = "settings.client.require-dpop-jkt";
    public static final String DPOP_REFRESH_TOKEN_ONLY = "settings.client.dpop-refresh-token-only";

    private ClientSecuritySettings() {}

    public static boolean requiresDpopProof(RegisteredClient client) {
        return Boolean.TRUE.equals(client.getClientSettings().getSetting(REQUIRE_DPOP_PROOF));
    }

    public static boolean requiresDpopJkt(RegisteredClient client) {
        return Boolean.TRUE.equals(client.getClientSettings().getSetting(REQUIRE_DPOP_JKT));
    }

    public static boolean requiresDpopForRefreshToken(RegisteredClient client) {
        return Boolean.TRUE.equals(client.getClientSettings().getSetting(DPOP_REFRESH_TOKEN_ONLY));
    }
}
