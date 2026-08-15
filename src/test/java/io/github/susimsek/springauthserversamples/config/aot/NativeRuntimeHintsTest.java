package io.github.susimsek.springauthserversamples.config.aot;

import static org.assertj.core.api.Assertions.assertThat;

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
}
