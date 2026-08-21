package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentId;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorizationConsentRepository
        extends JpaRepository<AuthorizationConsentEntity, AuthorizationConsentId> {

    Optional<AuthorizationConsentEntity> findByIdRegisteredClientIdAndIdPrincipalName(
            String registeredClientId, String principalName);

    void deleteByIdRegisteredClientIdAndIdPrincipalName(
            String registeredClientId, String principalName);

    long deleteByIdRegisteredClientId(String registeredClientId);

    long deleteByIdPrincipalName(String principalName);

    @Query(
            "select c from AuthorizationConsentEntity c where :query = '' or"
                    + " lower(c.id.principalName) like lower(concat('%', :query, '%')) or"
                    + " lower(c.id.registeredClientId) like lower(concat('%', :query, '%'))")
    Page<AuthorizationConsentEntity> search(@Param("query") String query, Pageable pageable);
}
