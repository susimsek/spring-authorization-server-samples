package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {

    @EntityGraph(attributePaths = "attributes")
    Optional<UserSessionEntity> findBySessionId(String sessionId);

    @EntityGraph(attributePaths = "attributes")
    List<UserSessionEntity> findAllByPrincipalNameAndExpiryTimeAfter(
            String principalName, long expiryTime);

    @Modifying
    @Query("delete from UserSessionEntity s where s.sessionId = :sessionId")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    @Modifying
    @Query("delete from UserSessionEntity s where s.expiryTime < :expiryTime")
    int deleteExpiredSessions(@Param("expiryTime") long expiryTime);
}
