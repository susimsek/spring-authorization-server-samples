package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
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
}
