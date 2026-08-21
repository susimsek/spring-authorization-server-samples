package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorizationRepository extends JpaRepository<AuthorizationEntity, String> {

    @Query(
            """
            select a
            from AuthorizationEntity a
            where a.state = :token
               or a.authorizationCodeValue = :token
               or a.accessTokenValue = :token
               or a.refreshTokenValue = :token
               or a.oidcIdTokenValue = :token
               or a.userCodeValue = :token
               or a.deviceCodeValue = :token
            """)
    Optional<AuthorizationEntity> findByToken(@Param("token") String token);

    Optional<AuthorizationEntity> findByState(String state);

    Optional<AuthorizationEntity> findByAuthorizationCodeValue(String authorizationCodeValue);

    Optional<AuthorizationEntity> findByAccessTokenValue(String accessTokenValue);

    Optional<AuthorizationEntity> findByRefreshTokenValue(String refreshTokenValue);

    Optional<AuthorizationEntity> findByOidcIdTokenValue(String oidcIdTokenValue);

    Optional<AuthorizationEntity> findByUserCodeValue(String userCodeValue);

    Optional<AuthorizationEntity> findByDeviceCodeValue(String deviceCodeValue);

    long deleteByPrincipalName(String principalName);

    long deleteByRegisteredClientId(String registeredClientId);

    long deleteBySessionId(String sessionId);

    long countByPrincipalName(String principalName);

    @Query(
            "select a.principalName as principalName, count(a) as authorizationCount "
                    + "from AuthorizationEntity a where a.principalName in :principalNames "
                    + "group by a.principalName")
    List<AuthorizationCount> countByPrincipalNameIn(
            @Param("principalNames") Collection<String> principalNames);

    long deleteByPrincipalNameAndRegisteredClientId(
            String principalName, String registeredClientId);

    interface AuthorizationCount {
        String getPrincipalName();

        long getAuthorizationCount();
    }
}
