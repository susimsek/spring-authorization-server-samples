package io.github.susimsek.springauthserversamples.config.session;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.session.JpaIndexedSessionRepository;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.MapSession;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SessionProperties.class)
@EnableSpringHttpSession
@EnableScheduling
public class SessionConfig {

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
}
