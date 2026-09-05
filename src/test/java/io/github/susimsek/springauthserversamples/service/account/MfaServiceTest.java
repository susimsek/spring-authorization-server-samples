package io.github.susimsek.springauthserversamples.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.service.security.TotpService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MfaServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final LoginSettingsRepository loginSettingsRepository =
            mock(LoginSettingsRepository.class);
    private final TotpService totpService = mock(TotpService.class);
    private final AdminAuditEventService auditEventService = mock(AdminAuditEventService.class);
    private final UserAccessInvalidationService invalidationService =
            mock(UserAccessInvalidationService.class);

    @Test
    void enablingMfaInvalidatesUserAccess() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("alice");
        user.setTotpSecret("SECRET");
        LoginSettingsEntity settings = new LoginSettingsEntity();
        settings.setOtpEnabled(true);
        settings.setOtpAlgorithm("SHA1");
        settings.setOtpDigits(6);
        settings.setOtpPeriodSeconds(30);
        settings.setOtpLookAheadWindow(1);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(loginSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));
        when(totpService.matches("SECRET", "123456", "SHA1", 6, 30, 1)).thenReturn(true);

        service().enable("alice", "123456");

        assertThat(user.isTotpEnabled()).isTrue();
        verify(invalidationService).invalidate("alice");
        verify(auditEventService).record("account.mfa.enabled", "user", "7");
    }

    @Test
    void setupReusesPendingSecret() {
        UserEntity user = new UserEntity();
        user.setUsername("alice");
        user.setTotpSecret("SECRET");
        user.setTotpEnabled(false);
        LoginSettingsEntity settings = new LoginSettingsEntity();
        settings.setOtpEnabled(true);
        settings.setOtpIssuer("Issuer");
        settings.setOtpAlgorithm("SHA1");
        settings.setOtpDigits(6);
        settings.setOtpPeriodSeconds(30);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(loginSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        assertThat(service().setup("alice").secret()).isEqualTo("SECRET");
        verify(totpService, org.mockito.Mockito.never()).newSecret();
    }

    @Test
    void invalidCodeDoesNotEnableMfaOrInvalidateAccess() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("alice");
        user.setTotpSecret("SECRET");
        LoginSettingsEntity settings = new LoginSettingsEntity();
        settings.setOtpEnabled(true);
        settings.setOtpAlgorithm("SHA1");
        settings.setOtpDigits(6);
        settings.setOtpPeriodSeconds(30);
        settings.setOtpLookAheadWindow(1);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(loginSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        assertThatThrownBy(() -> service().enable("alice", "000000"))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .isEqualTo(ApiErrorCode.INVALID_TOTP_CODE));

        assertThat(user.isTotpEnabled()).isFalse();
        verify(userRepository, never()).save(user);
        verify(invalidationService, never()).invalidate("alice");
    }

    @Test
    void invalidCodeDoesNotDisableMfaOrInvalidateAccess() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("alice");
        user.setTotpSecret("SECRET");
        user.setTotpEnabled(true);
        LoginSettingsEntity settings = new LoginSettingsEntity();
        settings.setOtpAlgorithm("SHA1");
        settings.setOtpDigits(6);
        settings.setOtpPeriodSeconds(30);
        settings.setOtpLookAheadWindow(1);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(loginSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        assertThatThrownBy(() -> service().disable("alice", "000000"))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .isEqualTo(ApiErrorCode.INVALID_TOTP_CODE));

        assertThat(user.isTotpEnabled()).isTrue();
        assertThat(user.getTotpSecret()).isEqualTo("SECRET");
        verify(userRepository, never()).save(user);
        verify(invalidationService, never()).invalidate("alice");
    }

    @Test
    void validReturnsFalseWhenMfaIsNotEnabledForTheUser() {
        UserEntity user = new UserEntity();
        user.setUsername("alice");
        user.setTotpSecret("SECRET");
        LoginSettingsEntity settings = new LoginSettingsEntity();
        settings.setOtpAlgorithm("SHA1");
        settings.setOtpDigits(6);
        settings.setOtpPeriodSeconds(30);
        settings.setOtpLookAheadWindow(1);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(loginSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        assertThat(service().valid("alice", "123456")).isFalse();
        verify(totpService, never()).matches("SECRET", "123456", "SHA1", 6, 30, 1);
    }

    private MfaService service() {
        return new MfaService(
                userRepository,
                loginSettingsRepository,
                totpService,
                auditEventService,
                invalidationService);
    }
}
