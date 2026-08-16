package io.github.susimsek.springauthserversamples.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
        @DefaultValue Cache cache, @DefaultValue AuthorizationServer authorizationServer) {

    public record Cache(@DefaultValue Caffeine caffeine) {}

    public record Caffeine(
            @DefaultValue("PT1H") Duration ttl,
            @DefaultValue("500") int initialCapacity,
            @DefaultValue("1000") long maximumSize) {}

    public record AuthorizationServer(@DefaultValue("http://127.0.0.1:9090") String issuer) {}
}
