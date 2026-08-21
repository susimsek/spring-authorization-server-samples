package io.github.susimsek.springauthserversamples.config.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

final class AdminConsoleRefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

    private static final String ADMIN_CONSOLE_CLIENT_ID = "admin-console";

    private final OAuth2RefreshTokenGenerator defaultGenerator = new OAuth2RefreshTokenGenerator();
    private final StringKeyGenerator refreshTokenGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);
    private final Clock clock = Clock.systemUTC();

    @Override
    public @Nullable OAuth2RefreshToken generate(OAuth2TokenContext context) {
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
            return null;
        }
        if (!ADMIN_CONSOLE_CLIENT_ID.equals(context.getRegisteredClient().getClientId())) {
            return defaultGenerator.generate(context);
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt =
                issuedAt.plus(
                        context.getRegisteredClient()
                                .getTokenSettings()
                                .getRefreshTokenTimeToLive());
        return new OAuth2RefreshToken(refreshTokenGenerator.generateKey(), issuedAt, expiresAt);
    }
}
