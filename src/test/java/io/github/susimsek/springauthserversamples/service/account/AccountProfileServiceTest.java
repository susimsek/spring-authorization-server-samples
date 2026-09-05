package io.github.susimsek.springauthserversamples.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileRequestDTO;
import io.github.susimsek.springauthserversamples.mapper.AccountProfileMapper;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.service.security.PasswordService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountProfileMapper accountProfileMapper;
    @Mock private PasswordService passwordService;
    @Mock private AdminAuditEventService auditEventService;
    @Mock private UserAccessInvalidationService userAccessInvalidationService;
    @Mock private UserActionService userActionService;

    @Test
    void normalizesAndUpdatesProfile() {
        UserEntity user = user();
        var request = new AccountProfileRequestDTO(" Alice ", " User ", " alice@example.test ");
        var expected =
                new AccountProfileDTO("alice", "Alice", "User", "alice@example.test", null, null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(accountProfileMapper.normalize(request))
                .thenReturn(new AccountProfileRequestDTO("Alice", "User", "alice@example.test"));
        when(accountProfileMapper.toDTO(user)).thenReturn(expected);

        assertThat(service().updateProfile("alice", request)).isEqualTo(expected);

        ArgumentCaptor<AccountProfileRequestDTO> normalized =
                ArgumentCaptor.forClass(AccountProfileRequestDTO.class);
        verify(accountProfileMapper)
                .updateNames(normalized.capture(), org.mockito.Mockito.same(user));
        assertThat(normalized.getValue())
                .isEqualTo(new AccountProfileRequestDTO("Alice", "User", "alice@example.test"));
        verify(auditEventService).record("account.profile.updated", "user", "7");
    }

    @Test
    void changesPasswordAfterVerifyingCurrentPassword() {
        UserEntity user = user();
        user.setPassword("encoded-old");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordService.matchesCurrentPassword("old-password", user)).thenReturn(true);
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            user.setPassword("encoded-new");
                            return null;
                        })
                .when(passwordService)
                .changePassword(user, "new-password");

        service().changePassword("alice", "old-password", "new-password");

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(auditEventService).record("account.password.updated", "user", "7");
    }

    @Test
    void rejectsAnInvalidCurrentPassword() {
        UserEntity user = user();
        user.setPassword("encoded-old");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service().changePassword("alice", "wrong", "new-password"))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ApiErrorCode.INVALID_CURRENT_PASSWORD);
    }

    private AccountProfileService service() {
        return new AccountProfileService(
                userRepository,
                accountProfileMapper,
                passwordService,
                auditEventService,
                userAccessInvalidationService,
                userActionService);
    }

    private static UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("alice");
        return user;
    }
}
