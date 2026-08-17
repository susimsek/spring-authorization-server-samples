package io.github.susimsek.springauthserversamples.session;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class SessionAttributeSerializer {

    private final JsonMapper objectMapper;

    public byte[] serialize(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize session attribute", ex);
        }
    }

    public Object deserialize(byte[] value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize session attribute", ex);
        }
    }
}
