package io.github.susimsek.springauthserversamples.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class AuthorizationServerJacksonConfig {

    @Bean
    JsonMapper authorizationServerJsonMapper() {
        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(getClass().getClassLoader()))
                .build();
    }
}
