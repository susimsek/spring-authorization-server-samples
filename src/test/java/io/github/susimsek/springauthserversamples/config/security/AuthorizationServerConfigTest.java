package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.security.LocalizedOAuth2ErrorResponseHandler;
import io.github.susimsek.springauthserversamples.security.OAuth2KeyJwkSource;
import io.github.susimsek.springauthserversamples.service.OAuth2KeyService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

class AuthorizationServerConfigTest {

    private final ApplicationProperties applicationProperties = applicationProperties();

    private final AuthorizationServerConfig config =
            new AuthorizationServerConfig(
                    applicationProperties, mock(LocalizedOAuth2ErrorResponseHandler.class));

    @Test
    void createsAuthorizationServerSettingsFromProperties() {
        AuthorizationServerSettings settings = config.authorizationServerSettings();

        assertThat(settings.getIssuer()).isEqualTo("https://issuer.example");
    }

    @Test
    void createsDatabaseBackedJwkSource() {
        OAuth2KeyService oauth2KeyService = mock(OAuth2KeyService.class);

        var jwkSource = config.jwkSource(oauth2KeyService);

        assertThat(jwkSource).isInstanceOf(OAuth2KeyJwkSource.class);
    }

    @Test
    void createsJwtDecoder() {
        OAuth2KeyService oauth2KeyService = mock(OAuth2KeyService.class);
        var jwkSource = config.jwkSource(oauth2KeyService);

        JwtDecoder jwtDecoder = config.jwtDecoder(jwkSource);

        assertThat(jwtDecoder).isNotNull();
    }

    private static ApplicationProperties applicationProperties() {
        return new ApplicationProperties(
                new ApplicationProperties.Cache(
                        new ApplicationProperties.Caffeine(
                                java.time.Duration.ofHours(1), 500, 1000)),
                new ApplicationProperties.Session("0 * * * * *"),
                new ApplicationProperties.AuthorizationServer("https://issuer.example"));
    }
}
