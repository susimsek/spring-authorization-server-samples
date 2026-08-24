package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.ClientScopeEntity;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientScopeRepository extends JpaRepository<ClientScopeEntity, String> {
    Optional<ClientScopeEntity> findByName(String name);

    boolean existsByName(String name);

    Page<ClientScopeEntity> findByNameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String name, String displayName, Pageable pageable);

    long countByNameIn(Collection<String> names);
}
