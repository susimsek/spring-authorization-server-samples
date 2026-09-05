package io.github.susimsek.springauthserversamples.service.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.PasswordHistoryEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.PasswordHistoryRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @Mock private PasswordHistoryRepository historyRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationProperties applicationProperties;

    @Test
    void enforcesComplexityAndLength() {
        when(applicationProperties.security()).thenReturn(security());
        when(historyRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        UserEntity user = user();

        assertThatThrownBy(() -> service().validateForNewPassword(user, "alllowercase123!"))
                .isInstanceOf(ApiException.class);

        service().validateForNewPassword(user, "StrongPassword1!");
    }

    @Test
    void rejectsRecentPasswordFromHistory() {
        when(applicationProperties.security()).thenReturn(security());
        when(historyRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(history("old-hash")));
        when(passwordEncoder.matches("StrongPassword1!", "current-hash")).thenReturn(false);
        when(passwordEncoder.matches("StrongPassword1!", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> service().validateForNewPassword(user(), "StrongPassword1!"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Password was recently used");
    }

    @Test
    void detectsExpiredPasswordsAndTrimsHistory() {
        ApplicationProperties.PasswordPolicy policy =
                new ApplicationProperties.PasswordPolicy(
                        12, 128, 1, 1, 1, 1, true, true, true, 1, 90, "password");
        when(applicationProperties.security())
                .thenReturn(
                        new ApplicationProperties.Security(
                                policy, new ApplicationProperties.BruteForce()));
        when(historyRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(history("one"), history("two")));

        UserEntity user = user();
        user.setPasswordChangedAt(Instant.now().minus(91, ChronoUnit.DAYS));

        service().recordChange(user, "new-hash");

        verify(historyRepository).save(any(PasswordHistoryEntity.class));
        verify(historyRepository).deleteAll(any());
        org.assertj.core.api.Assertions.assertThat(service().isExpired(user)).isTrue();
    }

    private PasswordPolicyService service() {
        return new PasswordPolicyService(historyRepository, passwordEncoder, applicationProperties);
    }

    private static ApplicationProperties.Security security() {
        return new ApplicationProperties.Security(
                new ApplicationProperties.PasswordPolicy(), new ApplicationProperties.BruteForce());
    }

    private static UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmail("alice@example.test");
        user.setPassword("current-hash");
        return user;
    }

    private static PasswordHistoryEntity history(String hash) {
        PasswordHistoryEntity history = new PasswordHistoryEntity();
        history.setPasswordHash(hash);
        return history;
    }
}
