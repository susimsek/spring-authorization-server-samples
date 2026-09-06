package io.github.susimsek.springauthserversamples.service.requiredaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.RequiredActionDefinitionEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.account.RecoveryCodesStatusDTO;
import io.github.susimsek.springauthserversamples.service.LoginSettingsService;
import io.github.susimsek.springauthserversamples.service.account.RecoveryCodeService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.service.security.TotpService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class StandardRequiredActionHandlerTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidProfileValues() {
        StandardRequiredActionHandler handler = new StandardRequiredActionHandler(validator);

        assertThatThrownBy(
                        () ->
                                handler.completeStandard(
                                        new UserEntity(),
                                        "UPDATE_PROFILE",
                                        Map.of(
                                                "firstName", "Ada",
                                                "lastName", "Lovelace",
                                                "email", "not-an-email")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsOversizedProfileValues() {
        StandardRequiredActionHandler handler = new StandardRequiredActionHandler(validator);

        assertThatThrownBy(
                        () ->
                                handler.completeStandard(
                                        new UserEntity(),
                                        "UPDATE_PROFILE",
                                        Map.of(
                                                "firstName", "A".repeat(101),
                                                "lastName", "Lovelace",
                                                "email", "ada@example.test")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void completesTotpRequiredActionAndInvalidatesAccess() {
        LoginSettingsService settings = mock(LoginSettingsService.class);
        TotpService totp = mock(TotpService.class);
        UserAccessInvalidationService invalidation = mock(UserAccessInvalidationService.class);
        when(settings.isOtpRequired()).thenReturn(true);
        when(settings.otpAlgorithm()).thenReturn("SHA1");
        when(settings.otpDigits()).thenReturn(6);
        when(settings.otpPeriodSeconds()).thenReturn(30);
        when(settings.otpLookAheadWindow()).thenReturn(1);
        when(totp.matchingCounter("SECRET", "123456", "SHA1", 6, 30, 1))
                .thenReturn(OptionalLong.of(100L));
        StandardRequiredActionHandler handler =
                new StandardRequiredActionHandler(
                        validator, null, null, invalidation, settings, totp);
        UserEntity user = new UserEntity();
        user.setUsername("alice");
        user.setTotpSecret("SECRET");
        RequiredActionDefinitionEntity definition = new RequiredActionDefinitionEntity();
        definition.setActionKey("CONFIGURE_TOTP");

        assertThat(handler.isPending(user, definition, false)).isTrue();
        handler.completeStandard(
                user, "CONFIGURE_TOTP", Map.of("code", "123456"), "current-session");

        assertThat(user.isTotpEnabled()).isTrue();
        verify(invalidation).invalidateOtherSessions("alice", "current-session");
    }

    @Test
    void invalidTotpCodeKeepsRequiredActionPending() {
        LoginSettingsService settings = mock(LoginSettingsService.class);
        TotpService totp = mock(TotpService.class);
        UserAccessInvalidationService invalidation = mock(UserAccessInvalidationService.class);
        when(settings.otpAlgorithm()).thenReturn("SHA1");
        when(settings.otpDigits()).thenReturn(6);
        when(settings.otpPeriodSeconds()).thenReturn(30);
        when(settings.otpLookAheadWindow()).thenReturn(1);
        StandardRequiredActionHandler handler =
                new StandardRequiredActionHandler(
                        validator, null, null, invalidation, settings, totp);
        UserEntity user = new UserEntity();
        user.setUsername("alice");
        user.setTotpSecret("SECRET");

        assertThatThrownBy(
                        () ->
                                handler.completeStandard(
                                        user,
                                        "CONFIGURE_TOTP",
                                        Map.of("code", "000000"),
                                        "current-session"))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .isEqualTo(ApiErrorCode.INVALID_TOTP_CODE));

        assertThat(user.isTotpEnabled()).isFalse();
        verify(invalidation, never()).invalidateOtherSessions("alice", "current-session");
    }

    @Test
    void acceptsRepeatedTotpCodeWhenCodeReuseIsEnabled() {
        LoginSettingsService settings = mock(LoginSettingsService.class);
        TotpService totp = mock(TotpService.class);
        UserAccessInvalidationService invalidation = mock(UserAccessInvalidationService.class);
        when(settings.isOtpCodeReusable()).thenReturn(true);
        when(settings.otpAlgorithm()).thenReturn("SHA1");
        when(settings.otpDigits()).thenReturn(6);
        when(settings.otpPeriodSeconds()).thenReturn(30);
        when(settings.otpLookAheadWindow()).thenReturn(1);
        when(totp.matchingCounter("SECRET", "123456", "SHA1", 6, 30, 1))
                .thenReturn(OptionalLong.of(100L));
        StandardRequiredActionHandler handler =
                new StandardRequiredActionHandler(
                        validator, null, null, invalidation, settings, totp);
        UserEntity user = new UserEntity();
        user.setUsername("alice");
        user.setTotpSecret("SECRET");
        RequiredActionDefinitionEntity definition = new RequiredActionDefinitionEntity();
        definition.setActionKey("CONFIGURE_TOTP");

        handler.completeStandard(
                user, "CONFIGURE_TOTP", Map.of("code", "123456"), "current-session");
        user.setTotpEnabled(false);
        handler.completeStandard(
                user, "CONFIGURE_TOTP", Map.of("code", "123456"), "current-session");

        assertThat(user.isTotpEnabled()).isTrue();
    }

    @Test
    void requiresRecoveryCodesToBeSavedBeforeCompletingAction() {
        RecoveryCodeService recoveryCodes = mock(RecoveryCodeService.class);
        UserEntity user = new UserEntity();
        user.setUsername("alice");
        when(recoveryCodes.status("alice")).thenReturn(new RecoveryCodesStatusDTO(12));
        StandardRequiredActionHandler handler =
                new StandardRequiredActionHandler(
                        validator, null, null, null, null, null, recoveryCodes);
        RequiredActionDefinitionEntity definition = new RequiredActionDefinitionEntity();
        definition.setActionKey("RECOVERY_CODES");

        assertThat(handler.isPending(user, definition, false)).isTrue();
        assertThatThrownBy(
                        () ->
                                handler.completeStandard(
                                        user, "RECOVERY_CODES", Map.of("accepted", false)))
                .isInstanceOf(ApiException.class);

        handler.completeStandard(user, "RECOVERY_CODES", Map.of("accepted", true));
        verify(recoveryCodes).status("alice");
    }
}
