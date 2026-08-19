package io.github.susimsek.springauthserversamples.config.security;

import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dedicated Jackson mapper for Spring Security and Authorization Server persistence.
 *
 * <p>This wrapper intentionally prevents the security-aware {@link JsonMapper} from becoming the
 * application's MVC/REST mapper.
 */
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
