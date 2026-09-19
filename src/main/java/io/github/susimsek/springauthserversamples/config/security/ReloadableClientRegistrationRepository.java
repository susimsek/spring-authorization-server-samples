package io.github.susimsek.springauthserversamples.config.security;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/** Client registration repository that can be refreshed by the admin settings service. */
public final class ReloadableClientRegistrationRepository implements ClientRegistrationRepository {

    private volatile Map<String, ClientRegistration> registrations;

    public ReloadableClientRegistrationRepository(Collection<ClientRegistration> registrations) {
        replace(registrations);
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    public void replace(Collection<ClientRegistration> values) {
        Map<String, ClientRegistration> updated = new LinkedHashMap<>();
        values.forEach(value -> updated.put(value.getRegistrationId(), value));
        registrations = Collections.unmodifiableMap(updated);
    }
}
