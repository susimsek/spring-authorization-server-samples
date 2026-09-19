package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.SocialProviderEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SocialProviderRepository
        extends JpaRepository<SocialProviderEntity, String>,
                JpaSpecificationExecutor<SocialProviderEntity> {

    Optional<SocialProviderEntity> findByRegistrationId(String registrationId);

    Optional<SocialProviderEntity> findByAliasIgnoreCase(String alias);

    boolean existsByAliasIgnoreCase(String alias);

    boolean existsByRegistrationId(String registrationId);
}
