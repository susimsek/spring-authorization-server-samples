package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.RegisteredClientEntity;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<RegisteredClientEntity, String> {

    String REGISTERED_CLIENT_BY_CLIENT_ID_CACHE = "registeredClientsByClientId";

    @Cacheable(cacheNames = REGISTERED_CLIENT_BY_CLIENT_ID_CACHE)
    Optional<RegisteredClientEntity> findByClientId(String clientId);
}
