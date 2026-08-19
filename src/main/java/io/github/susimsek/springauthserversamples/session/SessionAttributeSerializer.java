package io.github.susimsek.springauthserversamples.session;

import io.github.susimsek.springauthserversamples.config.security.SecurityJsonMapper;
import org.springframework.stereotype.Component;

@Component
public class SessionAttributeSerializer {

    private final SecurityJsonMapper securityJsonMapper;

    public SessionAttributeSerializer(SecurityJsonMapper securityJsonMapper) {
        this.securityJsonMapper = securityJsonMapper;
    }

    public byte[] serialize(Object value) {
        try {
            return securityJsonMapper.delegate().writeValueAsBytes(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize session attribute", ex);
        }
    }

    public Object deserialize(byte[] value) {
        try {
            return securityJsonMapper.delegate().readValue(value, Object.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize session attribute", ex);
        }
    }
}
