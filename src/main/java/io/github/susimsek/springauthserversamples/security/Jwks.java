package io.github.susimsek.springauthserversamples.security;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Jwks {

    private static final String RSA_ALGORITHM = "RSA";

    public RSAKey generateRsa() {
        KeyPair keyPair = KeyGeneratorUtils.generateRsaKey();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    public RSAKey loadRsa(String publicKey, String privateKey, String keyId) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);

            RSAPublicKey rsaPublicKey =
                    (RSAPublicKey)
                            keyFactory.generatePublic(
                                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));

            RSAPrivateKey rsaPrivateKey =
                    (RSAPrivateKey)
                            keyFactory.generatePrivate(
                                    new PKCS8EncodedKeySpec(
                                            Base64.getDecoder().decode(privateKey)));

            return new RSAKey.Builder(rsaPublicKey).privateKey(rsaPrivateKey).keyID(keyId).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create RSA JWK", ex);
        }
    }
}
