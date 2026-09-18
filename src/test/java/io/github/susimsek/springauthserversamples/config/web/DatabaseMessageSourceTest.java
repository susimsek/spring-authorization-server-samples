package io.github.susimsek.springauthserversamples.config.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.LocalizationMessageOverrideEntity;
import io.github.susimsek.springauthserversamples.repository.LocalizationMessageOverrideRepository;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;

@ExtendWith(MockitoExtension.class)
class DatabaseMessageSourceTest {

    @Mock private LocalizationMessageOverrideRepository overrideRepository;

    @Test
    void resolvesEmailOverridesFromEmailBundle() {
        LocalizationMessageOverrideEntity override = new LocalizationMessageOverrideEntity();
        override.setMessageValue("Test e-postası");
        when(overrideRepository.findByLocaleAndBundleAndMessageKey(
                        "tr", "email", "mail.test.subject"))
                .thenReturn(Optional.of(override));

        StaticMessageSource bundled = new StaticMessageSource();
        bundled.addMessage("mail.test.subject", Locale.forLanguageTag("tr"), "Bundled");

        var source = new DatabaseMessageSource(bundled, overrideRepository);

        assertThat(source.getMessage("mail.test.subject", null, Locale.forLanguageTag("tr")))
                .isEqualTo("Test e-postası");
    }
}
