package io.github.susimsek.springauthserversamples.service.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
}
