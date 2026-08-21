package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;

class AdminConsoleRefreshTokenGeneratorTest {

    private final AdminConsoleRefreshTokenGenerator generator =
            new AdminConsoleRefreshTokenGenerator();

    @Test
    void returnsNullForNonRefreshTokenRequests() {
        OAuth2TokenContext context = mock(OAuth2TokenContext.class);
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);

        assertThat(generator.generate(context)).isNull();
    }

    @Test
    void delegatesForNonAdminConsoleClients() {
        OAuth2RefreshToken token = generator.generate(context("regular-client"));

        assertThat(token).isNotNull();
    }

    @Test
    void generatesAdminConsoleRefreshTokenWithExpectedTtl() {
        OAuth2RefreshToken token = generator.generate(context("admin-console"));

        assertThat(token).isNotNull();
        assertThat(token.getTokenValue()).isNotBlank();
        assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt()))
                .isEqualTo(Duration.ofHours(2));
    }

    private static OAuth2TokenContext context(String clientId) {
        OAuth2TokenContext context = mock(OAuth2TokenContext.class);
        when(context.getTokenType()).thenReturn(OAuth2TokenType.REFRESH_TOKEN);
        when(context.getRegisteredClient())
                .thenReturn(
                        RegisteredClient.withId("id")
                                .clientId(clientId)
                                .clientAuthenticationMethod(
                                        org.springframework.security.oauth2.core
                                                .ClientAuthenticationMethod.NONE)
                                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                                .tokenSettings(
                                        TokenSettings.builder()
                                                .refreshTokenTimeToLive(Duration.ofHours(2))
                                                .build())
                                .build());
        return context;
    }
}
