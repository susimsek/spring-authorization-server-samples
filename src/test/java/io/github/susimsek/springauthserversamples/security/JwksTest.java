package io.github.susimsek.springauthserversamples.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwksTest {

    @Test
    void generatesRsaKey() throws Exception {
        RSAKey rsaKey = Jwks.generateRsa();

        assertThat(rsaKey).isNotNull();
        assertThat(rsaKey.getKeyID()).isNotBlank();
        assertThat(rsaKey.toRSAPublicKey()).isNotNull();
        assertThat(rsaKey.toRSAPrivateKey()).isNotNull();
        assertThat(rsaKey.isPrivate()).isTrue();
    }

    @Test
    void loadsRsaKey() throws Exception {
        KeyPair keyPair = KeyGeneratorUtils.generateRsaKey();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        String encodedPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String encodedPrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        RSAKey rsaKey = Jwks.loadRsa(encodedPublicKey, encodedPrivateKey, "test-key");

        assertThat(rsaKey).isNotNull();
        assertThat(rsaKey.getKeyID()).isEqualTo("test-key");
        assertThat(rsaKey.toRSAPublicKey()).isEqualTo(publicKey);
        assertThat(rsaKey.toRSAPrivateKey()).isEqualTo(privateKey);
    }

    @Test
    void throwsExceptionWhenRsaKeyIsInvalid() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> Jwks.loadRsa("invalid-public-key", "invalid-private-key", "test-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to create RSA JWK");
    }
}
