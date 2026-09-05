package io.github.susimsek.springauthserversamples.service.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/** Small RFC 6238 TOTP implementation used for account MFA. */
@Service
public class TotpService {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String newSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return encode(bytes);
    }

    public boolean matches(
            String secret,
            String code,
            String algorithm,
            int digits,
            int periodSeconds,
            int lookAheadWindow) {
        return matchesAt(
                secret, code, algorithm, digits, periodSeconds, lookAheadWindow, Instant.now());
    }

    boolean matchesAt(
            String secret,
            String code,
            String algorithm,
            int digits,
            int periodSeconds,
            int lookAheadWindow,
            Instant now) {
        if (secret == null || code == null || !code.matches("\\d{" + digits + "}")) {
            return false;
        }
        long counter = now.getEpochSecond() / periodSeconds;
        for (long offset = -lookAheadWindow; offset <= lookAheadWindow; offset++) {
            if (generate(secret, counter + offset, algorithm, digits).equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String otpauthUri(
            String issuer,
            String account,
            String secret,
            String algorithm,
            int digits,
            int periodSeconds) {
        String label = urlEncode(issuer) + ":" + urlEncode(account);
        return "otpauth://totp/"
                + label
                + "?secret="
                + secret
                + "&issuer="
                + urlEncode(issuer)
                + "&algorithm="
                + algorithm
                + "&digits="
                + digits
                + "&period="
                + periodSeconds;
    }

    private static String generate(String secret, long counter, String algorithm, int digits) {
        try {
            byte[] hash =
                    hmac(
                            algorithm,
                            decode(secret),
                            ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0xf;
            int binary =
                    ((hash[offset] & 0x7f) << 24)
                            | ((hash[offset + 1] & 0xff) << 16)
                            | ((hash[offset + 2] & 0xff) << 8)
                            | (hash[offset + 3] & 0xff);
            int modulo = digits == 8 ? 100_000_000 : 1_000_000;
            return String.format(Locale.ROOT, "%0" + digits + "d", binary % modulo);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("TOTP algorithm is unavailable", ex);
        }
    }

    private static byte[] hmac(String algorithm, byte[] secret, byte[] counter)
            throws GeneralSecurityException {
        String normalizedAlgorithm = algorithm.replace("-", "").toUpperCase(Locale.ROOT);
        Mac mac = Mac.getInstance("Hmac" + normalizedAlgorithm);
        mac.init(new SecretKeySpec(secret, mac.getAlgorithm()));
        return mac.doFinal(counter);
    }

    private static String encode(byte[] bytes) {
        StringBuilder result = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                result.append(BASE32.charAt((buffer >> (bits -= 5)) & 31));
            }
        }
        if (bits > 0) {
            result.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        }
        return result.toString();
    }

    private static byte[] decode(String value) {
        String normalized = value.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        byte[] result = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bits = 0;
        int index = 0;
        for (char character : normalized.toCharArray()) {
            int digit = BASE32.indexOf(character);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base32 secret");
            }
            buffer = (buffer << 5) | digit;
            bits += 5;
            if (bits >= 8) {
                result[index++] = (byte) ((buffer >> (bits -= 8)) & 0xff);
            }
        }
        return result;
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
