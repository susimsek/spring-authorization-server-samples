package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<UserSessionEntity> findAllByExpiryTimeAfter(long expiryTime);

    long countByExpiryTimeAfter(long expiryTime);

    @Query(
            "select s from UserSessionEntity s where s.expiryTime > :expiryTime and (:query = '' or"
                    + " lower(s.principalName) like lower(concat('%', :query, '%')))")
    Page<UserSessionEntity> findActiveSessions(
            @Param("expiryTime") long expiryTime, @Param("query") String query, Pageable pageable);

    @Query(
            "select s from UserSessionEntity s where (:status = 'all' or (:status = 'active' and"
                + " s.expiryTime > :now) or (:status = 'expired' and s.expiryTime <= :now)) and"
                + " (:query = '' or lower(s.principalName) like lower(concat('%', :query, '%')))")
    Page<UserSessionEntity> findSessions(
            @Param("now") long now,
            @Param("query") String query,
            @Param("status") String status,
            Pageable pageable);

    @Query(
            "select s from UserSessionEntity s where s.sessionId in :sessionIds and (:status ="
                + " 'all' or (:status = 'active' and s.expiryTime > :now) or (:status = 'expired'"
                + " and s.expiryTime <= :now)) and (:query = '' or lower(s.principalName) like"
                + " lower(concat('%', :query, '%')))")
    Page<UserSessionEntity> findSessionsBySessionIdIn(
            @Param("now") long now,
            @Param("query") String query,
            @Param("status") String status,
            @Param("sessionIds") Collection<String> sessionIds,
            Pageable pageable);

    @Query(
            "select s from UserSessionEntity s where s.expiryTime > :expiryTime and"
                    + " s.principalName = :principalName")
    Page<UserSessionEntity> findActiveSessionsByPrincipalName(
            @Param("expiryTime") long expiryTime,
            @Param("principalName") String principalName,
            Pageable pageable);

    @Query(
            "select s from UserSessionEntity s where s.expiryTime > :expiryTime and"
                    + " s.sessionId in :sessionIds")
    Page<UserSessionEntity> findActiveSessionsBySessionIdIn(
            @Param("expiryTime") long expiryTime,
            @Param("sessionIds") Collection<String> sessionIds,
            Pageable pageable);

    @Modifying
    @Query("delete from UserSessionEntity s where s.sessionId = :sessionId")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    long deleteByPrincipalName(String principalName);

    @Modifying
    @Query("delete from UserSessionEntity s where s.expiryTime < :expiryTime")
    int deleteExpiredSessions(@Param("expiryTime") long expiryTime);
}
