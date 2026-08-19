package io.github.susimsek.springauthserversamples.config.security;

import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;

public final class SecurityJsonMapper {

    private final JsonMapper delegate;

    public SecurityJsonMapper(ClassLoader classLoader) {
        this.delegate =
                JsonMapper.builder()
                        .addModules(SecurityJacksonModules.getModules(classLoader))
                        .build();
    }

    public JsonMapper delegate() {
        return delegate;
    }
}
