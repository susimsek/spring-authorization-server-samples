package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.EventListenerProviderEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventListenerProviderRepository
        extends JpaRepository<EventListenerProviderEntity, String> {

    boolean existsByNameIgnoreCase(String name);

    Optional<EventListenerProviderEntity> findByNameIgnoreCase(String name);
}
