package io.github.susimsek.springauthserversamples.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for optional OAuth2 social login providers. */
@ConfigurationProperties(prefix = "app.social-login")
public class SocialLoginProperties {

    private boolean enabled;
    private String encryptionKey = "";
    private Provider google = new Provider();
    private Provider github = new Provider();
    private Provider linkedin = new Provider();
    private Provider microsoft = new Provider();

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String encryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public Provider google() {
        return google;
    }

    public void setGoogle(Provider google) {
        this.google = google;
    }

    public Provider github() {
        return github;
    }

    public void setGithub(Provider github) {
        this.github = github;
    }

    public Provider linkedin() {
        return linkedin;
    }

    public void setLinkedin(Provider linkedin) {
        this.linkedin = linkedin;
    }

    public Provider microsoft() {
        return microsoft;
    }

    public void setMicrosoft(Provider microsoft) {
        this.microsoft = microsoft;
    }

    public static class Provider {

        private String clientId = "";
        private String clientSecret = "";

        public String clientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String clientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public boolean configured() {
            return clientId != null
                    && !clientId.isBlank()
                    && clientSecret != null
                    && !clientSecret.isBlank();
        }
    }
}
