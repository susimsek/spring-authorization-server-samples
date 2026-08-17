package io.github.susimsek.springauthserversamples.session;

import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.stereotype.Component;

@Component
public class SessionAttributeSerializer {

    private final SerializingConverter serializer = new SerializingConverter();
    private final DeserializingConverter deserializer = new DeserializingConverter();

    public byte[] serialize(Object value) {
        return serializer.convert(value);
    }

    public Object deserialize(byte[] value) {
        return deserializer.convert(value);
    }
}
