package io.github.susimsek.springauthserversamples.config.aot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

class NativeRuntimeHintsTest {

    @Test
    void registersApplicationResources() {
        RuntimeHints hints = new RuntimeHints();

        new NativeRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.resource().forResource("i18n/messages.properties"))
                .accepts(hints);
    }

    @Test
    void includesProjectNativeReflectConfig() throws IOException {
        String json =
                readResource(
                        "/META-INF/native-image/org.springframework.security/"
                                + "spring-security-oauth2-authorization-server/"
                                + "reflect-config.json");

        assertThat(json)
                .contains(
                        "org.springframework.security.oauth2.server.authorization."
                                + "settings.OAuth2TokenFormat")
                .contains(
                        "org.springframework.security.oauth2.server.authorization."
                                + "jackson.OAuth2TokenFormatMixin")
                .contains("java.util.Collections$SingletonList");
    }

    private String readResource(String location) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(location)) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
