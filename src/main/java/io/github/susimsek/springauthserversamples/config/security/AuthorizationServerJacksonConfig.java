package io.github.susimsek.springauthserversamples.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AuthorizationServerJacksonConfig {

    @Bean
    SecurityJsonMapper securityJsonMapper() {
        return new SecurityJsonMapper(getClass().getClassLoader());
    }
}
