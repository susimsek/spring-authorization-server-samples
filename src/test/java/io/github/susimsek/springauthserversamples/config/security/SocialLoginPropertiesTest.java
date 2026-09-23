package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SocialLoginPropertiesTest {

    @Test
    void bindsAllProviderProperties() {
        final SocialLoginProperties properties = new SocialLoginProperties();
        SocialLoginProperties.Provider google = new SocialLoginProperties.Provider();
        SocialLoginProperties.Provider github = new SocialLoginProperties.Provider();
        final SocialLoginProperties.Provider linkedin = new SocialLoginProperties.Provider();
        final SocialLoginProperties.Provider microsoft = new SocialLoginProperties.Provider();
        google.setClientId("google-id");
        google.setClientSecret("google-secret");
        github.setClientId("github-id");
        github.setClientSecret("github-secret");
        linkedin.setClientId("linkedin-id");
        linkedin.setClientSecret("linkedin-secret");
        microsoft.setClientId("microsoft-id");
        microsoft.setClientSecret("microsoft-secret");

        properties.setEnabled(true);
        properties.setEncryptionKey("key");
        properties.setGoogle(google);
        properties.setGithub(github);
        properties.setLinkedin(linkedin);
        properties.setMicrosoft(microsoft);

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.encryptionKey()).isEqualTo("key");
        assertThat(properties.google()).isSameAs(google);
        assertThat(properties.github()).isSameAs(github);
        assertThat(properties.linkedin()).isSameAs(linkedin);
        assertThat(properties.microsoft()).isSameAs(microsoft);
        assertThat(google.configured()).isTrue();
        assertThat(github.configured()).isTrue();
        assertThat(linkedin.configured()).isTrue();
        assertThat(microsoft.configured()).isTrue();
    }
}
