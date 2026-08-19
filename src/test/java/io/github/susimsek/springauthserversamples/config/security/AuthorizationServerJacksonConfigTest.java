package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthorizationServerJacksonConfigTest {

    @Test
    void createsDedicatedSecurityJsonMapper() {
        AuthorizationServerJacksonConfig config = new AuthorizationServerJacksonConfig();

        SecurityJsonMapper mapper = config.securityJsonMapper();

        assertThat(mapper).isNotNull();
        assertThat(mapper.delegate()).isNotNull();
    }
}
