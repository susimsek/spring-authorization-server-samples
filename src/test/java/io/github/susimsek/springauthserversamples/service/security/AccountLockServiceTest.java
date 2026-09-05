package io.github.susimsek.springauthserversamples.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountLockServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserAccessInvalidationService invalidationService;
    @Mock private ApplicationProperties applicationProperties;

    @Test
    void locksRapidFailuresAndUnlocksAfterTheWait() {
        ApplicationProperties.BruteForce policy =
                new ApplicationProperties.BruteForce(
                        true,
                        5,
                        Duration.ofMinutes(1),
                        Duration.ofMinutes(1),
                        Duration.ofMinutes(1),
                        Duration.ofMinutes(15),
                        Duration.ofHours(12),
                        3,
                        false,
                        30,
                        5);
        when(applicationProperties.security())
                .thenReturn(
                        new ApplicationProperties.Security(
                                new ApplicationProperties.PasswordPolicy(), policy));
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("alice");
        user.setEnabled(true);
        when(userRepository.findForLoginUpdate("alice")).thenReturn(Optional.of(user));
        when(userRepository.findForActionById(7L)).thenReturn(Optional.of(user));

        AccountLockService service =
                new AccountLockService(userRepository, invalidationService, applicationProperties);
        service.recordFailure("alice", "127.0.0.1");
        service.recordFailure("alice", "127.0.0.1");

        assertThat(user.getLockedUntil()).isAfter(Instant.now());
        assertThat(service.isLocked(user, Instant.now())).isTrue();

        user.setLockedUntil(Instant.now().minusSeconds(1));
        assertThat(service.isLocked(user, Instant.now())).isFalse();
        service.unlock(7L);

        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).findForActionById(7L);
    }
}
