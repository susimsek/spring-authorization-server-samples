package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.security.LocalizedAccessDeniedHandler;
import io.github.susimsek.springauthserversamples.security.LocalizedAuthenticationEntryPoint;
import io.github.susimsek.springauthserversamples.security.LocalizedOAuth2ErrorResponseHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

class SecurityConfigTest {

    private final SecurityConfig config =
            new SecurityConfig(
                    applicationProperties(),
                    org.mockito.Mockito.mock(LocalizedOAuth2ErrorResponseHandler.class),
                    org.mockito.Mockito.mock(LocalizedAuthenticationEntryPoint.class),
                    org.mockito.Mockito.mock(LocalizedAccessDeniedHandler.class));

    @Test
    void createsPasswordEncoder() {
        assertThat(config.passwordEncoder()).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void createsAuthorizationServerSettingsFromProperties() {
        AuthorizationServerSettings settings = config.authorizationServerSettings();

        assertThat(settings.getIssuer()).isEqualTo("https://issuer.example");
    }

    @Test
    void createsRsaKeyAndJwkSource() throws Exception {
        RSAKey rsaKey = config.rsaKey();

        assertThat(rsaKey.getKeyID()).isNotBlank();
        assertThat(rsaKey.toRSAPublicKey()).isNotNull();
        assertThat(rsaKey.toRSAPrivateKey()).isNotNull();

        var jwkSource = config.jwkSource(rsaKey);
        List<com.nimbusds.jose.jwk.JWK> keys =
                jwkSource.get(new JWKSelector(new JWKMatcher.Builder().build()), null);

        assertThat(keys).hasSize(1);
        assertThat(keys.getFirst().getKeyID()).isEqualTo(rsaKey.getKeyID());
    }

    @Test
    void createsJwtDecoder() throws Exception {
        JwtDecoder jwtDecoder = config.jwtDecoder(config.rsaKey());

        assertThat(jwtDecoder).isNotNull();
    }

    private static ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getAuthorizationServer().setIssuer("https://issuer.example");
        return applicationProperties;
    }
}
