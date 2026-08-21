package io.github.susimsek.springauthserversamples.config.session;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.config.security.SecurityJsonMapper;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import io.github.susimsek.springauthserversamples.session.JpaIndexedSessionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.MapSession;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SessionProperties.class)
@EnableSpringHttpSession
@EnableScheduling
public class SessionConfig {

    @Bean("springSessionConversionService")
    ConversionService springSessionConversionService(SecurityJsonMapper securityJsonMapper) {
        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter(
                Object.class,
                byte[].class,
                new SerializingConverter(new JsonSerializer(securityJsonMapper)));
        conversionService.addConverter(
                byte[].class,
                Object.class,
                new DeserializingConverter(new JsonDeserializer(securityJsonMapper)));
        return conversionService;
    }

    @Bean
    JpaIndexedSessionRepository sessionRepository(
            UserSessionRepository userSessionRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("springSessionConversionService") ConversionService conversionService) {
        JpaIndexedSessionRepository repository =
                new JpaIndexedSessionRepository(userSessionRepository, transactionManager);
        repository.setConversionService(conversionService);
        return repository;
    }

    @Bean
    SessionCleanupScheduler sessionCleanupScheduler(
            JpaIndexedSessionRepository sessionRepository,
            TaskScheduler taskScheduler,
            SessionProperties sessionProperties,
            ApplicationProperties applicationProperties) {
        Duration timeout = sessionProperties.getTimeout();
        if (timeout != null) {
            sessionRepository.setDefaultMaxInactiveInterval(timeout);
        } else {
            sessionRepository.setDefaultMaxInactiveInterval(
                    MapSession.DEFAULT_MAX_INACTIVE_INTERVAL);
        }
        return new SessionCleanupScheduler(
                sessionRepository, taskScheduler, applicationProperties.session().cleanupCron());
    }

    private static final class JsonSerializer implements Serializer<Object> {

        private final SecurityJsonMapper securityJsonMapper;

        private JsonSerializer(SecurityJsonMapper securityJsonMapper) {
            this.securityJsonMapper = securityJsonMapper;
        }

        @Override
        public void serialize(Object object, OutputStream outputStream) throws IOException {
            securityJsonMapper.delegate().writeValue(outputStream, object);
        }
    }

    private static final class JsonDeserializer implements Deserializer<Object> {

        private final SecurityJsonMapper securityJsonMapper;

        private JsonDeserializer(SecurityJsonMapper securityJsonMapper) {
            this.securityJsonMapper = securityJsonMapper;
        }

        @Override
        public Object deserialize(InputStream inputStream) throws IOException {
            return securityJsonMapper.delegate().readValue(inputStream, Object.class);
        }
    }
}
