package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springauthserversamples.service.SocialProviderSettingsService.ProviderCredentials;
import java.util.List;
import org.junit.jupiter.api.Test;

class SocialLoginConfigTest {

    @Test
    void keepsOAuthClientRepositoryUsableBeforeAdminCredentialsAreEntered() {
        assertThat(SocialLoginConfig.registrations(List.of())).isEmpty();
    }

    @Test
    void createsOnlyConfiguredRegistrations() {
        List<ProviderCredentials> providers =
                List.of(
                        new ProviderCredentials("google", "google-id", "google-secret"),
                        new ProviderCredentials("microsoft", "", ""));

        assertThat(SocialLoginConfig.registrations(providers))
                .extracting("registrationId")
                .containsExactly("google");
    }
}
