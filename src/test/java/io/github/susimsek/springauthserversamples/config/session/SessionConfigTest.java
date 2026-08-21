package io.github.susimsek.springauthserversamples.config.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.config.security.SecurityJsonMapper;
import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import io.github.susimsek.springauthserversamples.session.JpaIndexedSessionRepository;
import io.github.susimsek.springauthserversamples.session.JpaSession;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.session.MapSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class SessionConfigTest {

    private final SessionConfig config = new SessionConfig();

    @Test
    void springSessionConversionServiceRoundTripsJsonCompatiblePayloads() {
        ConversionService conversionService =
                config.springSessionConversionService(
                        new SecurityJsonMapper(getClass().getClassLoader()));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", "admin");
        payload.put("enabled", true);

        byte[] serialized = conversionService.convert(payload, byte[].class);
        Object deserialized = conversionService.convert(serialized, Object.class);

        assertThat(serialized).isNotEmpty();
        assertThat(deserialized)
                .isInstanceOf(Map.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("username", "admin")
                .containsEntry("enabled", true);
    }

    @Test
    void sessionRepositoryUsesProvidedConversionService() {
        ConversionService conversionService =
                config.springSessionConversionService(
                        new SecurityJsonMapper(getClass().getClassLoader()));
        UserSessionRepository userSessionRepository = mock(UserSessionRepository.class);
        InMemorySessionStore store = new InMemorySessionStore();
        when(userSessionRepository.findBySessionId(anyString()))
                .thenAnswer(invocation -> store.findBySessionId(invocation.getArgument(0)));
        when(userSessionRepository.findAllByPrincipalNameAndExpiryTimeAfter(anyString(), anyLong()))
                .thenAnswer(
                        invocation ->
                                store.findAllByPrincipalNameAndExpiryTimeAfter(
                                        invocation.getArgument(0), invocation.getArgument(1)));
        when(userSessionRepository.save(any(UserSessionEntity.class)))
                .thenAnswer(invocation -> store.save(invocation.getArgument(0)));
        when(userSessionRepository.deleteBySessionId(anyString()))
                .thenAnswer(invocation -> store.deleteBySessionId(invocation.getArgument(0)));
        when(userSessionRepository.deleteExpiredSessions(anyLong()))
                .thenAnswer(invocation -> store.deleteExpiredSessions(invocation.getArgument(0)));

        JpaIndexedSessionRepository repository =
                config.sessionRepository(
                        userSessionRepository, new NoOpTransactionManager(), conversionService);
        JpaSession session = repository.createSession();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenant", "internal");
        payload.put("count", 2);
        session.setAttribute("payload", payload);

        repository.save(session);

        assertThat(
                        repository
                                .findById(session.getId())
                                .<Map<String, Object>>getAttribute("payload"))
                .containsEntry("tenant", "internal")
                .containsEntry("count", 2);
    }

    @Test
    void sessionCleanupSchedulerAppliesConfiguredTimeout() {
        JpaIndexedSessionRepository repository =
                new JpaIndexedSessionRepository(
                        mock(UserSessionRepository.class), new NoOpTransactionManager());
        SessionProperties sessionProperties = new SessionProperties();
        sessionProperties.setTimeout(Duration.ofMinutes(5));

        SessionCleanupScheduler scheduler =
                config.sessionCleanupScheduler(
                        repository,
                        mock(TaskScheduler.class),
                        sessionProperties,
                        new ApplicationProperties(
                                new ApplicationProperties.Cache(
                                        new ApplicationProperties.Caffeine(
                                                Duration.ofHours(1), 500, 1000)),
                                new ApplicationProperties.Session("-"),
                                new ApplicationProperties.AuthorizationServer(
                                        "http://127.0.0.1:9090")));

        assertThat(repository.createSession().getMaxInactiveInterval())
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(scheduler).isNotNull();
    }

    @Test
    void sessionCleanupSchedulerFallsBackToDefaultTimeoutWhenUnset() {
        JpaIndexedSessionRepository repository =
                new JpaIndexedSessionRepository(
                        mock(UserSessionRepository.class), new NoOpTransactionManager());

        config.sessionCleanupScheduler(
                repository,
                mock(TaskScheduler.class),
                new SessionProperties(),
                new ApplicationProperties(
                        new ApplicationProperties.Cache(
                                new ApplicationProperties.Caffeine(Duration.ofHours(1), 500, 1000)),
                        new ApplicationProperties.Session("-"),
                        new ApplicationProperties.AuthorizationServer("http://127.0.0.1:9090")));

        assertThat(repository.createSession().getMaxInactiveInterval())
                .isEqualTo(MapSession.DEFAULT_MAX_INACTIVE_INTERVAL);
    }

    private static final class NoOpTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {}

        @Override
        public void rollback(TransactionStatus status) {}
    }

    private static final class InMemorySessionStore {

        private final Map<String, UserSessionEntity> bySessionId = new LinkedHashMap<>();

        private Optional<UserSessionEntity> findBySessionId(String sessionId) {
            return Optional.ofNullable(bySessionId.get(sessionId)).map(this::copyOf);
        }

        private List<UserSessionEntity> findAllByPrincipalNameAndExpiryTimeAfter(
                String principalName, long expiryTime) {
            return bySessionId.values().stream()
                    .filter(entity -> principalName.equals(entity.getPrincipalName()))
                    .filter(entity -> entity.getExpiryTime() > expiryTime)
                    .map(this::copyOf)
                    .toList();
        }

        private UserSessionEntity save(UserSessionEntity entity) {
            UserSessionEntity copy = copyOf(entity);
            bySessionId.put(copy.getSessionId(), copy);
            return copyOf(copy);
        }

        private int deleteBySessionId(String sessionId) {
            return bySessionId.remove(sessionId) == null ? 0 : 1;
        }

        private int deleteExpiredSessions(long expiryTime) {
            List<String> expiredIds =
                    bySessionId.values().stream()
                            .filter(entity -> entity.getExpiryTime() < expiryTime)
                            .map(UserSessionEntity::getSessionId)
                            .toList();
            expiredIds.forEach(bySessionId::remove);
            return expiredIds.size();
        }

        private UserSessionEntity copyOf(UserSessionEntity entity) {
            UserSessionEntity copy = new UserSessionEntity();
            copy.setPrimaryId(entity.getPrimaryId());
            copy.setSessionId(entity.getSessionId());
            copy.setCreationTime(entity.getCreationTime());
            copy.setLastAccessTime(entity.getLastAccessTime());
            copy.setMaxInactiveInterval(entity.getMaxInactiveInterval());
            copy.setExpiryTime(entity.getExpiryTime());
            copy.setPrincipalName(entity.getPrincipalName());
            Map<String, byte[]> attributes = new LinkedHashMap<>();
            entity.getAttributes().forEach((key, value) -> attributes.put(key, value.clone()));
            copy.setAttributes(attributes);
            return copy;
        }
    }
}
