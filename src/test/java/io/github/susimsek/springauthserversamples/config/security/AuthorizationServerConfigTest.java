package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.security.KeyGeneratorUtils;
import io.github.susimsek.springauthserversamples.security.LocalizedAccessDeniedHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedAuthenticationEntryPoint;
import io.github.susimsek.springauthserversamples.security.LocalizedOAuth2ErrorResponseHandler;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

class AuthorizationServerConfigTest {

    private final ApplicationProperties applicationProperties = applicationProperties();

    private final AuthorizationServerConfig config =
            new AuthorizationServerConfig(
                    applicationProperties,
                    mock(LocalizedOAuth2ErrorResponseHandler.class),
                    mock(LocalizedAuthenticationEntryPoint.class),
                    mock(LocalizedAccessDeniedHandler.class));

    @Test
    void createsAuthorizationServerSettingsFromProperties() {
        AuthorizationServerSettings settings = config.authorizationServerSettings();

        assertThat(settings.getIssuer()).isEqualTo("https://issuer.example");
    }

    @Test
    void createsJwkSourceWithConfiguredRsaKey() throws Exception {
        var jwkSource = config.jwkSource();

        List<com.nimbusds.jose.jwk.JWK> keys =
                jwkSource.get(new JWKSelector(new JWKMatcher.Builder().build()), null);

        assertThat(keys).hasSize(1);

        RSAKey rsaKey = keys.getFirst().toRSAKey();

        assertThat(rsaKey.getKeyID()).isEqualTo("test-key");
        assertThat(rsaKey.toRSAPublicKey()).isNotNull();
        assertThat(rsaKey.toRSAPrivateKey()).isNotNull();
    }

    @Test
    void createsJwtDecoder() {
        var jwkSource = config.jwkSource();

        JwtDecoder jwtDecoder = config.jwtDecoder(jwkSource);

        assertThat(jwtDecoder).isNotNull();
    }

    private static ApplicationProperties applicationProperties() {
        KeyPair keyPair = KeyGeneratorUtils.generateRsaKey();

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        ApplicationProperties properties = new ApplicationProperties();

        properties.getAuthorizationServer().setIssuer("https://issuer.example");

        properties.getAuthorizationServer().getJwk().setPublicKey(publicKey);

        properties.getAuthorizationServer().getJwk().setPrivateKey(privateKey);

        properties.getAuthorizationServer().getJwk().setKeyId("test-key");

        return properties;
    }
}
