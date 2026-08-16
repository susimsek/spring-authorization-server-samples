package io.github.susimsek.springauthserversamples.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

class KeyGeneratorUtilsTest {

    @Test
    void generatesRsaKeyPair() {
        KeyPair keyPair = KeyGeneratorUtils.generateRsaKey();

        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic()).isInstanceOf(RSAPublicKey.class);
        assertThat(keyPair.getPrivate()).isInstanceOf(RSAPrivateKey.class);
    }

    @Test
    void generates2048BitRsaKey() {
        KeyPair keyPair = KeyGeneratorUtils.generateRsaKey();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        assertThat(publicKey.getModulus().bitLength()).isEqualTo(2048);
    }

    @Test
    void generatesDifferentKeyPairs() {
        KeyPair firstKeyPair = KeyGeneratorUtils.generateRsaKey();
        KeyPair secondKeyPair = KeyGeneratorUtils.generateRsaKey();

        assertThat(firstKeyPair.getPublic()).isNotEqualTo(secondKeyPair.getPublic());
        assertThat(firstKeyPair.getPrivate()).isNotEqualTo(secondKeyPair.getPrivate());
    }
}
