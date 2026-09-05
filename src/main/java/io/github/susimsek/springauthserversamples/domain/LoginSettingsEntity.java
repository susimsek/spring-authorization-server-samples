package io.github.susimsek.springauthserversamples.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Application-wide switches that control the public login experience. */
@Entity
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

    @Column(name = "remember_me_enabled", nullable = false)
    private boolean rememberMeEnabled;

    @Column(name = "login_with_email", nullable = false)
    private boolean loginWithEmail;

    @Column(name = "verify_email", nullable = false)
    private boolean verifyEmail;

    @Column(name = "session_timeout_minutes", nullable = false)
    private int sessionTimeoutMinutes;

    @Column(name = "password_minimum_length", nullable = false)
    private int passwordMinimumLength;

    @Column(name = "brute_force_enabled", nullable = false)
    private boolean bruteForceEnabled;

    @Column(name = "brute_force_max_failures", nullable = false)
    private int bruteForceMaxFailures;

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
}
