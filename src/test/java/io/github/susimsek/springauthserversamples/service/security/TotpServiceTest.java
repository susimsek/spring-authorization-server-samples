package io.github.susimsek.springauthserversamples.service.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private final TotpService service = new TotpService();

    @Test
    void matchesRfc6238Sha1Vector() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

        assertThat(
                        service.matchesAt(
                                secret, "94287082", "SHA1", 8, 30, 0, Instant.ofEpochSecond(59)))
                .isTrue();
    }

    @Test
    void acceptsHyphenatedAlgorithmName() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

        assertThat(
                        service.matchesAt(
                                secret, "94287082", "SHA-1", 8, 30, 0, Instant.ofEpochSecond(59)))
                .isTrue();
    }

    @Test
    void rejectsCodesOutsideConfiguredLength() {
        assertThat(
                        service.matchesAt(
                                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                                "123456",
                                "SHA1",
                                8,
                                30,
                                0,
                                Instant.ofEpochSecond(59)))
                .isFalse();
    }

    @Test
    void rejectsIncorrectCodeWithConfiguredLength() {
        assertThat(
                        service.matchesAt(
                                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                                "00000000",
                                "SHA1",
                                8,
                                30,
                                0,
                                Instant.ofEpochSecond(59)))
                .isFalse();
    }

    @Test
    void acceptsOnlyCodesInsideConfiguredClockWindow() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

        assertThat(
                        service.matchesAt(
                                secret, "94287082", "SHA1", 8, 30, 1, Instant.ofEpochSecond(89)))
                .isTrue();
        assertThat(
                        service.matchesAt(
                                secret, "94287082", "SHA1", 8, 30, 1, Instant.ofEpochSecond(119)))
                .isFalse();
    }

    @Test
    void generatesBackendQrCodeDataUri() {
        String dataUri = service.qrCodeDataUri("otpauth://totp/Authorization%20Server:admin");

        assertThat(dataUri).startsWith("data:image/png;base64,");
        byte[] png = Base64.getDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1));
        assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
    }
}
