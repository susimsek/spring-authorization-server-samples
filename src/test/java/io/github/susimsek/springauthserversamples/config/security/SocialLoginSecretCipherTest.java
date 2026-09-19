package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SocialLoginSecretCipherTest {

    @Test
    void encryptsAndDecryptsSecretWithoutPersistingPlainText() {
        SocialLoginProperties properties = new SocialLoginProperties();
        properties.setEncryptionKey("test-encryption-key");
        SocialLoginSecretCipher cipher = new SocialLoginSecretCipher(properties);

        String encrypted = cipher.encrypt("client-secret");

        assertThat(encrypted).startsWith("v1:").doesNotContain("client-secret");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("client-secret");
    }

    @Test
    void rejectsDecryptingWithAnotherKey() {
        SocialLoginProperties properties = new SocialLoginProperties();
        properties.setEncryptionKey("test-encryption-key");
        SocialLoginSecretCipher cipher = new SocialLoginSecretCipher(properties);
        String encrypted = cipher.encrypt("client-secret");

        properties.setEncryptionKey("another-key");

        assertThatThrownBy(() -> cipher.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The social login secret could not be decrypted");
    }
}
