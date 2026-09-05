package io.github.susimsek.springauthserversamples.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.security.PasswordService;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountRegistrationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthorityRepository authorityRepository;
    @Mock private PasswordService passwordService;
    @Mock private UserActionService userActionService;
    @Mock private AdminAuditEventService auditEventService;
    @Mock private ApplicationProperties applicationProperties;

    @Test
    void registersAnEnabledUserWithTheDefaultRole() {
        AuthorityEntity userRole = new AuthorityEntity();
        userRole.setName(AuthoritiesConstants.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("alice@example.test")).thenReturn(false);
        when(authorityRepository.findByName(AuthoritiesConstants.USER))
                .thenReturn(Optional.of(userRole));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            invocation.<UserEntity>getArgument(0).setPassword("encoded-password");
                            return null;
                        })
                .when(passwordService)
                .setInitialPassword(any(UserEntity.class), org.mockito.Mockito.eq("password-123"));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(
                        invocation -> {
                            UserEntity user = invocation.getArgument(0);
                            user.setId(7L);
                            return user;
                        });
        when(applicationProperties.mail())
                .thenReturn(new ApplicationProperties.Mail(false, "", ""));

        service()
                .register(
                        " alice ",
                        " Ada ",
                        " Lovelace ",
                        " ALICE@EXAMPLE.TEST ",
                        "password-123",
                        "password-123",
                        Locale.ENGLISH);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getFirstName()).isEqualTo("Ada");
        assertThat(saved.getLastName()).isEqualTo("Lovelace");
        assertThat(saved.getEmail()).isEqualTo("alice@example.test");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getAuthorities()).containsExactly(userRole);
        verify(auditEventService).record("user.registered", "user", "7");
        verify(userActionService, never()).sendForCurrentUser(any(), any(), any());
    }

    private AccountRegistrationService service() {
        return new AccountRegistrationService(
                userRepository,
                authorityRepository,
                passwordService,
                userActionService,
                auditEventService,
                applicationProperties);
    }
}
