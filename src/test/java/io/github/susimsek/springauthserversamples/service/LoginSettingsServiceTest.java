package io.github.susimsek.springauthserversamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminLoginSettingsRequestDTO;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginSettingsServiceTest {

    private final LoginSettingsRepository repository = mock(LoginSettingsRepository.class);
    private final AdminAuditEventService auditEventService = mock(AdminAuditEventService.class);
    private LoginSettingsEntity settings;
    private LoginSettingsService service;

    @BeforeEach
    void setUp() {
        settings = settings();
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        service = new LoginSettingsService(repository, auditEventService);
    }

    @Test
    void exposesPublicAdminAndPolicyValues() {
        assertThat(service.publicLoginSettings().userRegistration()).isTrue();
        assertThat(service.adminLoginSettings().passwordMinimumLength()).isEqualTo(12);
        assertThat(service.isRememberMeEnabled()).isTrue();
        assertThat(service.isUserRegistrationEnabled()).isTrue();
        assertThat(service.isForgotPasswordEnabled()).isTrue();
        assertThat(service.passwordResetOtpMode()).isEqualTo("if-configured");
        assertThat(service.passwordResetTokenLifespan()).isEqualTo(Duration.ofSeconds(600));
        assertThat(service.passwordResetResendCooldown()).isEqualTo(Duration.ofSeconds(30));
        assertThat(service.isLoginWithEmailEnabled()).isTrue();
        assertThat(service.isVerifyEmailEnabled()).isTrue();
        assertThat(service.emailUpdateReauthenticationAge()).isEqualTo(Duration.ofMinutes(5));
        assertThat(service.isSocialProviderEnabled("GOOGLE")).isTrue();
        assertThat(service.isSocialProviderEnabled("github")).isFalse();
        assertThat(service.isSocialProviderEnabled("unknown")).isFalse();
        assertThat(service.isBruteForceEnabled()).isTrue();
        assertThat(service.passwordMinimumLength()).isEqualTo(12);
        assertThat(service.bruteForceMaxFailures()).isEqualTo(5);
        assertThat(service.bruteForceMaxSecondaryFailures()).isEqualTo(2);
        assertThat(service.mfaVerificationTimeout()).isEqualTo(Duration.ofSeconds(300));
        assertThat(service.passwordPolicy().maximumLength()).isEqualTo(128);
        assertThat(service.bruteForcePolicy().maxFailures()).isEqualTo(5);
        assertThat(service.webAuthnPolicy(false).rpName()).isEqualTo("Sample");
        assertThat(service.webAuthnPolicy(true).rpName()).isEqualTo("Passwordless");
        assertThat(service.isOtpAddRecoveryCodesEnabled()).isTrue();
        assertThat(service.isOtpEnabled()).isTrue();
        assertThat(service.isOtpRequired()).isTrue();
        assertThat(service.otpIssuer()).isEqualTo("Issuer");
        assertThat(service.otpAlgorithm()).isEqualTo("SHA256");
        assertThat(service.otpDigits()).isEqualTo(6);
        assertThat(service.otpPeriodSeconds()).isEqualTo(30);
        assertThat(service.otpLookAheadWindow()).isEqualTo(1);
        assertThat(service.isOtpCodeReusable()).isTrue();
    }

    @Test
    void updatesSettingsAndRecordsAudit() {
        AdminLoginSettingsRequestDTO request =
                new AdminLoginSettingsRequestDTO(
                        false,
                        false,
                        true,
                        false,
                        false,
                        45,
                        14,
                        false,
                        7,
                        1,
                        true,
                        false,
                        "New Issuer",
                        "SHA512",
                        8,
                        60,
                        2,
                        true,
                        false,
                        4);

        assertThat(service.update(request).userRegistration()).isFalse();
        assertThat(settings.getSessionTimeoutMinutes()).isEqualTo(45);
        assertThat(settings.getOtpIssuer()).isEqualTo("New Issuer");
        verify(repository).save(settings);
        verify(auditEventService).record("login.settings.updated", "login-settings", "default");
    }

    @Test
    void rejectsMissingSettings() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(service::publicLoginSettings)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not initialized");
    }

    private static LoginSettingsEntity settings() {
        LoginSettingsEntity value = new LoginSettingsEntity();
        value.setUserRegistrationEnabled(true);
        value.setForgotPasswordEnabled(true);
        value.setPasswordResetOtpMode("if-configured");
        value.setPasswordResetTokenLifespanSeconds(600);
        value.setPasswordResetResendCooldownSeconds(30);
        value.setRememberMeEnabled(true);
        value.setLoginWithEmail(true);
        value.setVerifyEmail(true);
        value.setEmailUpdateReauthenticationMinutes(5);
        value.setWebAuthnRpName("Sample");
        value.setWebAuthnRpId("");
        value.setWebAuthnSignatureAlgorithms("ES256,RS256");
        value.setWebAuthnAttestation("none");
        value.setWebAuthnAuthenticatorAttachment("any");
        value.setWebAuthnResidentKey("preferred");
        value.setWebAuthnUserVerification("preferred");
        value.setWebAuthnTimeoutSeconds(300);
        value.setWebAuthnAvoidSameAuthenticator(true);
        value.setWebAuthnAcceptableAaguids("");
        value.setWebAuthnPasswordlessRpName("Passwordless");
        value.setWebAuthnPasswordlessRpId("");
        value.setWebAuthnPasswordlessSignatureAlgorithms("ES256");
        value.setWebAuthnPasswordlessAttestation("none");
        value.setWebAuthnPasswordlessAuthenticatorAttachment("platform");
        value.setWebAuthnPasswordlessResidentKey("required");
        value.setWebAuthnPasswordlessUserVerification("required");
        value.setWebAuthnPasswordlessTimeoutSeconds(300);
        value.setWebAuthnPasswordlessAvoidSameAuthenticator(true);
        value.setWebAuthnPasswordlessAcceptableAaguids("");
        value.setGoogleLoginEnabled(true);
        value.setGithubLoginEnabled(false);
        value.setLinkedinLoginEnabled(true);
        value.setMicrosoftLoginEnabled(false);
        value.setSessionTimeoutMinutes(30);
        value.setPasswordMinimumLength(12);
        value.setPasswordMaximumLength(128);
        value.setPasswordMinimumUppercase(1);
        value.setPasswordMinimumLowercase(1);
        value.setPasswordMinimumDigits(1);
        value.setPasswordMinimumSpecialCharacters(1);
        value.setPasswordRejectUsername(true);
        value.setPasswordRejectEmail(true);
        value.setPasswordRejectCommonPasswords(true);
        value.setPasswordHistorySize(5);
        value.setPasswordExpirationDays(90);
        value.setPasswordCommonPasswords("password");
        value.setBruteForceEnabled(true);
        value.setBruteForceMaxFailures(5);
        value.setBruteForceMaxSecondaryFailures(2);
        value.setMfaVerificationTimeoutSeconds(300);
        value.setBruteForceQuickLoginWindowMillis(1000);
        value.setBruteForceMinimumQuickLoginWaitSeconds(60);
        value.setBruteForceWaitIncrementSeconds(60);
        value.setBruteForceMaxWaitSeconds(900);
        value.setBruteForceFailureResetTimeSeconds(43200);
        value.setBruteForceMaxTemporaryLockouts(3);
        value.setBruteForceIpRequestsPerMinute(30);
        value.setBruteForceUsernameIpRequestsPerMinute(5);
        value.setOtpEnabled(true);
        value.setOtpRequired(true);
        value.setOtpIssuer("Issuer");
        value.setOtpAlgorithm("SHA256");
        value.setOtpDigits(6);
        value.setOtpPeriodSeconds(30);
        value.setOtpLookAheadWindow(1);
        value.setOtpCodeReusable(true);
        value.setOtpAddRecoveryCodes(true);
        value.setRecoveryCodeWarningThreshold(2);
        return value;
    }
}
