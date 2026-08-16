package io.github.susimsek.springauthserversamples.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.RSAKey;
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
    void generates2048BitRsaKey() throws Exception {
        RSAKey rsaKey = Jwks.generateRsa();

        assertThat(rsaKey.toRSAPublicKey().getModulus().bitLength()).isEqualTo(2048);
    }

    @Test
    void generatesDifferentRsaKeys() {
        RSAKey firstKey = Jwks.generateRsa();
        RSAKey secondKey = Jwks.generateRsa();

        assertThat(firstKey.getKeyID()).isNotEqualTo(secondKey.getKeyID());
        assertThat(firstKey.getModulus()).isNotEqualTo(secondKey.getModulus());
    }
}
