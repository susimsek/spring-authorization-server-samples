package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationConsentRepository
        extends JpaRepository<AuthorizationConsentEntity, AuthorizationConsentId> {

    Optional<AuthorizationConsentEntity> findByIdRegisteredClientIdAndIdPrincipalName(
            String registeredClientId, String principalName);

    void deleteByIdRegisteredClientIdAndIdPrincipalName(
            String registeredClientId, String principalName);
}
