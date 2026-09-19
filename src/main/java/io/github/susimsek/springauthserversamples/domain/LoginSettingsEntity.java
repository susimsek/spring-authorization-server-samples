package io.github.susimsek.springauthserversamples.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** Application-wide switches that control the public login experience. */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@Table(name = "login_settings")
public class LoginSettingsEntity {

    @Id private Long id;

    @Column(name = "user_registration_enabled", nullable = false)
    private boolean userRegistrationEnabled;

    @Column(name = "forgot_password_enabled", nullable = false)
    private boolean forgotPasswordEnabled;

    @Column(name = "password_reset_otp_mode", nullable = false, length = 20)
    private String passwordResetOtpMode;

    @Column(name = "password_reset_token_lifespan_seconds", nullable = false)
    private int passwordResetTokenLifespanSeconds;

    @Column(name = "password_reset_resend_cooldown_seconds", nullable = false)
    private int passwordResetResendCooldownSeconds;

    @Column(name = "remember_me_enabled", nullable = false)
    private boolean rememberMeEnabled;

    @Column(name = "passkeys_enabled", nullable = false)
    private boolean passkeysEnabled;

    @Column(name = "login_with_email", nullable = false)
    private boolean loginWithEmail;

    @Column(name = "verify_email", nullable = false)
    private boolean verifyEmail;

    @Column(name = "webauthn_mediation", nullable = false, length = 20)
    private String webAuthnMediation;

    @Column(name = "email_update_reauthentication_minutes", nullable = false)
    private int emailUpdateReauthenticationMinutes;

    @Column(name = "google_login_enabled", nullable = false)
    private boolean googleLoginEnabled;

    @Column(name = "github_login_enabled", nullable = false)
    private boolean githubLoginEnabled;

    @Column(name = "linkedin_login_enabled", nullable = false)
    private boolean linkedinLoginEnabled;

    @Column(name = "microsoft_login_enabled", nullable = false)
    private boolean microsoftLoginEnabled;

    @Column(name = "google_client_id", length = 500)
    private String googleClientId;

    @Column(name = "google_client_secret_encrypted", length = 2000)
    private String googleClientSecretEncrypted;

    @Column(name = "github_client_id", length = 500)
    private String githubClientId;

    @Column(name = "github_client_secret_encrypted", length = 2000)
    private String githubClientSecretEncrypted;

    @Column(name = "linkedin_client_id", length = 500)
    private String linkedinClientId;

    @Column(name = "linkedin_client_secret_encrypted", length = 2000)
    private String linkedinClientSecretEncrypted;

    @Column(name = "microsoft_client_id", length = 500)
    private String microsoftClientId;

    @Column(name = "microsoft_client_secret_encrypted", length = 2000)
    private String microsoftClientSecretEncrypted;

    @Column(name = "session_timeout_minutes", nullable = false)
    private int sessionTimeoutMinutes;

    @Column(name = "password_minimum_length", nullable = false)
    private int passwordMinimumLength;

    @Column(name = "brute_force_enabled", nullable = false)
    private boolean bruteForceEnabled;

    @Column(name = "brute_force_max_failures", nullable = false)
    private int bruteForceMaxFailures;

    @Column(name = "brute_force_max_secondary_failures", nullable = false)
    private int bruteForceMaxSecondaryFailures;

    @Column(name = "mfa_verification_timeout_seconds", nullable = false)
    private int mfaVerificationTimeoutSeconds;

    @Column(name = "password_maximum_length", nullable = false)
    private int passwordMaximumLength;

    @Column(name = "password_minimum_uppercase", nullable = false)
    private int passwordMinimumUppercase;

    @Column(name = "password_minimum_lowercase", nullable = false)
    private int passwordMinimumLowercase;

    @Column(name = "password_minimum_digits", nullable = false)
    private int passwordMinimumDigits;

    @Column(name = "password_minimum_special_characters", nullable = false)
    private int passwordMinimumSpecialCharacters;

    @Column(name = "password_reject_username", nullable = false)
    private boolean passwordRejectUsername;

    @Column(name = "password_reject_email", nullable = false)
    private boolean passwordRejectEmail;

    @Column(name = "password_reject_common_passwords", nullable = false)
    private boolean passwordRejectCommonPasswords;

    @Column(name = "password_history_size", nullable = false)
    private int passwordHistorySize;

    @Column(name = "password_expiration_days", nullable = false)
    private int passwordExpirationDays;

    @Column(name = "password_common_passwords", nullable = false, length = 4000)
    private String passwordCommonPasswords;

    @Column(name = "brute_force_quick_login_window_millis", nullable = false)
    private int bruteForceQuickLoginWindowMillis;

    @Column(name = "brute_force_minimum_quick_login_wait_seconds", nullable = false)
    private int bruteForceMinimumQuickLoginWaitSeconds;

    @Column(name = "brute_force_wait_increment_seconds", nullable = false)
    private int bruteForceWaitIncrementSeconds;

    @Column(name = "brute_force_max_wait_seconds", nullable = false)
    private int bruteForceMaxWaitSeconds;

    @Column(name = "brute_force_failure_reset_time_seconds", nullable = false)
    private int bruteForceFailureResetTimeSeconds;

    @Column(name = "brute_force_max_temporary_lockouts", nullable = false)
    private int bruteForceMaxTemporaryLockouts;

    @Column(name = "brute_force_permanent_lockout", nullable = false)
    private boolean bruteForcePermanentLockout;

    @Column(name = "brute_force_ip_requests_per_minute", nullable = false)
    private int bruteForceIpRequestsPerMinute;

    @Column(name = "brute_force_username_ip_requests_per_minute", nullable = false)
    private int bruteForceUsernameIpRequestsPerMinute;

    @Column(name = "otp_enabled", nullable = false)
    private boolean otpEnabled;

    @Column(name = "otp_required", nullable = false)
    private boolean otpRequired;

    @Column(name = "otp_issuer", nullable = false, length = 100)
    private String otpIssuer;

    @Column(name = "otp_algorithm", nullable = false, length = 10)
    private String otpAlgorithm;

    @Column(name = "otp_digits", nullable = false)
    private int otpDigits;

    @Column(name = "otp_period_seconds", nullable = false)
    private int otpPeriodSeconds;

    @Column(name = "otp_look_ahead_window", nullable = false)
    private int otpLookAheadWindow;

    @Column(name = "otp_code_reusable", nullable = false)
    private boolean otpCodeReusable;

    @Column(name = "otp_add_recovery_codes", nullable = false)
    private boolean otpAddRecoveryCodes;

    @Column(name = "recovery_code_warning_threshold", nullable = false)
    private int recoveryCodeWarningThreshold;
}
