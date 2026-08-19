package io.github.susimsek.springauthserversamples.session;

import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.session.DelegatingIndexResolver;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.FlushMode;
import org.springframework.session.IndexResolver;
import org.springframework.session.MapSession;
import org.springframework.session.PrincipalNameIndexResolver;
import org.springframework.session.SaveMode;
import org.springframework.session.Session;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

public class JpaIndexedSessionRepository implements FindByIndexNameSessionRepository<JpaSession> {

    public static final String DEFAULT_CLEANUP_CRON = "0 * * * * *";

    private final UserSessionRepository sessionRepository;
    private ConversionService conversionService = defaultConversionService();
    private final TransactionTemplate transactionTemplate;
    private IndexResolver<Session> indexResolver =
            new DelegatingIndexResolver<>(new PrincipalNameIndexResolver<>());

    private Duration defaultMaxInactiveInterval = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL;
    private FlushMode flushMode = FlushMode.ON_SAVE;
    private SaveMode saveMode = SaveMode.ON_SET_ATTRIBUTE;

    public JpaIndexedSessionRepository(
            UserSessionRepository sessionRepository,
            PlatformTransactionManager transactionManager) {
        this.sessionRepository = sessionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public JpaSession createSession() {
        MapSession delegate = new MapSession();
        delegate.setMaxInactiveInterval(defaultMaxInactiveInterval);
        JpaSession session = new JpaSession(delegate, this, true);
        if (flushMode == FlushMode.IMMEDIATE) {
            save(session);
        }
        return session;
    }

    @Override
    public void save(JpaSession session) {
        transactionTemplate.executeWithoutResult(status -> saveInTransaction(session));
        session.markPersisted();
    }

    @Override
    public JpaSession findById(String id) {
        return transactionTemplate.execute(
                status -> {
                    Optional<UserSessionEntity> result = sessionRepository.findBySessionId(id);
                    if (result.isEmpty()) {
                        return null;
                    }
                    JpaSession session = toSession(result.get());
                    if (session.isExpired()) {
                        sessionRepository.deleteBySessionId(id);
                        return null;
                    }
                    return session;
                });
    }

    @Override
    public void deleteById(String id) {
        transactionTemplate.executeWithoutResult(status -> sessionRepository.deleteBySessionId(id));
    }

    @Override
    public Map<String, JpaSession> findByIndexNameAndIndexValue(
            String indexName, String indexValue) {
        if (!PRINCIPAL_NAME_INDEX_NAME.equals(indexName)) {
            return Collections.emptyMap();
        }

        List<JpaSession> sessions =
                transactionTemplate.execute(
                        status ->
                                sessionRepository
                                        .findAllByPrincipalNameAndExpiryTimeAfter(
                                                indexValue, System.currentTimeMillis())
                                        .stream()
                                        .map(this::toSession)
                                        .toList());

        if (sessions == null) {
            return Collections.emptyMap();
        }
        return sessions.stream().collect(Collectors.toMap(JpaSession::getId, Function.identity()));
    }

    public void cleanUpExpiredSessions() {
        transactionTemplate.executeWithoutResult(
                status -> sessionRepository.deleteExpiredSessions(System.currentTimeMillis()));
    }

    public void setDefaultMaxInactiveInterval(Duration defaultMaxInactiveInterval) {
        Assert.notNull(defaultMaxInactiveInterval, "defaultMaxInactiveInterval cannot be null");
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    public void setFlushMode(FlushMode flushMode) {
        Assert.notNull(flushMode, "flushMode cannot be null");
        this.flushMode = flushMode;
    }

    public void setIndexResolver(IndexResolver<Session> indexResolver) {
        Assert.notNull(indexResolver, "indexResolver cannot be null");
        this.indexResolver = indexResolver;
    }

    public void setSaveMode(SaveMode saveMode) {
        Assert.notNull(saveMode, "saveMode cannot be null");
        this.saveMode = saveMode;
    }

    public void setConversionService(ConversionService conversionService) {
        Assert.notNull(conversionService, "conversionService cannot be null");
        this.conversionService = conversionService;
    }

    void flushIfRequired(JpaSession session) {
        if (flushMode == FlushMode.IMMEDIATE) {
            save(session);
        }
    }

    void attributeRead(JpaSession session, String attributeName, Object attributeValue) {
        if (saveMode == SaveMode.ON_GET_ATTRIBUTE && attributeValue != null) {
            session.getDelta().put(attributeName, attributeValue);
        }
    }

    private void saveInTransaction(JpaSession session) {
        UserSessionEntity entity = findEntityForSave(session).orElseGet(UserSessionEntity::new);
        if (entity.getPrimaryId() == null) {
            entity.setPrimaryId(UUID.randomUUID().toString());
        }

        MapSession delegate = session.getDelegate();
        entity.setSessionId(session.getId());
        entity.setCreationTime(delegate.getCreationTime().toEpochMilli());
        entity.setLastAccessTime(delegate.getLastAccessedTime().toEpochMilli());
        entity.setMaxInactiveInterval((int) delegate.getMaxInactiveInterval().getSeconds());
        entity.setExpiryTime(expiryTime(delegate));
        entity.setPrincipalName(
                indexResolver.resolveIndexesFor(delegate).get(PRINCIPAL_NAME_INDEX_NAME));

        if (session.isNew() || saveMode == SaveMode.ALWAYS) {
            entity.getAttributes().clear();
            delegate.getAttributeNames()
                    .forEach(
                            attributeName -> {
                                Object attribute = delegate.getAttribute(attributeName);
                                if (attribute != null) {
                                    entity.getAttributes()
                                            .put(attributeName, serializeAttribute(attribute));
                                }
                            });
        } else {
            session.getDelta()
                    .forEach(
                            (attributeName, attribute) -> {
                                if (attribute == null) {
                                    entity.getAttributes().remove(attributeName);
                                } else {
                                    entity.getAttributes()
                                            .put(attributeName, serializeAttribute(attribute));
                                }
                            });
        }

        sessionRepository.save(entity);
    }

    private Optional<UserSessionEntity> findEntityForSave(JpaSession session) {
        Optional<UserSessionEntity> current = sessionRepository.findBySessionId(session.getId());
        if (current.isPresent() || session.getOriginalId().equals(session.getId())) {
            return current;
        }
        return sessionRepository.findBySessionId(session.getOriginalId());
    }

    private JpaSession toSession(UserSessionEntity entity) {
        MapSession delegate = new MapSession(entity.getSessionId());
        delegate.setCreationTime(Instant.ofEpochMilli(entity.getCreationTime()));
        delegate.setLastAccessedTime(Instant.ofEpochMilli(entity.getLastAccessTime()));
        delegate.setMaxInactiveInterval(Duration.ofSeconds(entity.getMaxInactiveInterval()));
        entity.getAttributes()
                .forEach(
                        (attributeName, bytes) ->
                                delegate.setAttribute(attributeName, deserializeAttribute(bytes)));
        return new JpaSession(delegate, this, false);
    }

    private byte[] serializeAttribute(Object attribute) {
        byte[] bytes = conversionService.convert(attribute, byte[].class);
        Assert.state(bytes != null, "Session attribute serialization returned null");
        return bytes;
    }

    private Object deserializeAttribute(byte[] bytes) {
        return conversionService.convert(bytes, Object.class);
    }

    private static ConversionService defaultConversionService() {
        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter(Object.class, byte[].class, new SerializingConverter());
        conversionService.addConverter(byte[].class, Object.class, new DeserializingConverter());
        return conversionService;
    }

    private static long expiryTime(Session session) {
        if (session.getMaxInactiveInterval().isNegative()) {
            return Long.MAX_VALUE;
        }
        return session.getLastAccessedTime().plus(session.getMaxInactiveInterval()).toEpochMilli();
    }
}
