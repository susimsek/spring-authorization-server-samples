package io.github.susimsek.springauthserversamples.mapper;

import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.account.LoginSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminLoginSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminLoginSettingsRequestDTO;
import java.util.Locale;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LoginSettingsMapper {

    @Mapping(target = "userRegistration", source = "userRegistrationEnabled")
    @Mapping(target = "forgotPassword", source = "forgotPasswordEnabled")
    @Mapping(target = "rememberMe", source = "rememberMeEnabled")
    LoginSettingsDTO toPublicDTO(LoginSettingsEntity source);

    @Mapping(target = "userRegistration", source = "userRegistrationEnabled")
    @Mapping(target = "forgotPassword", source = "forgotPasswordEnabled")
    @Mapping(target = "rememberMe", source = "rememberMeEnabled")
    @Mapping(target = "loginWithEmail", source = "loginWithEmail")
    @Mapping(target = "verifyEmail", source = "verifyEmail")
    @Mapping(target = "sessionTimeoutMinutes", source = "sessionTimeoutMinutes")
    @Mapping(target = "passwordMinimumLength", source = "passwordMinimumLength")
    @Mapping(target = "bruteForceEnabled", source = "bruteForceEnabled")
    @Mapping(target = "bruteForceMaxFailures", source = "bruteForceMaxFailures")
    @Mapping(target = "bruteForceMaxSecondaryFailures", source = "bruteForceMaxSecondaryFailures")
    @Mapping(target = "mfaVerificationTimeoutSeconds", source = "mfaVerificationTimeoutSeconds")
    @Mapping(target = "passwordMaximumLength", source = "passwordMaximumLength")
    @Mapping(target = "passwordMinimumUppercase", source = "passwordMinimumUppercase")
    @Mapping(target = "passwordMinimumLowercase", source = "passwordMinimumLowercase")
    @Mapping(target = "passwordMinimumDigits", source = "passwordMinimumDigits")
    @Mapping(
            target = "passwordMinimumSpecialCharacters",
            source = "passwordMinimumSpecialCharacters")
    @Mapping(target = "passwordRejectUsername", source = "passwordRejectUsername")
    @Mapping(target = "passwordRejectEmail", source = "passwordRejectEmail")
    @Mapping(target = "passwordRejectCommonPasswords", source = "passwordRejectCommonPasswords")
    @Mapping(target = "passwordHistorySize", source = "passwordHistorySize")
    @Mapping(target = "passwordExpirationDays", source = "passwordExpirationDays")
    @Mapping(target = "passwordCommonPasswords", source = "passwordCommonPasswords")
    @Mapping(
            target = "bruteForceQuickLoginWindowMillis",
            source = "bruteForceQuickLoginWindowMillis")
    @Mapping(
            target = "bruteForceMinimumQuickLoginWaitSeconds",
            source = "bruteForceMinimumQuickLoginWaitSeconds")
    @Mapping(target = "bruteForceWaitIncrementSeconds", source = "bruteForceWaitIncrementSeconds")
    @Mapping(target = "bruteForceMaxWaitSeconds", source = "bruteForceMaxWaitSeconds")
    @Mapping(
            target = "bruteForceFailureResetTimeSeconds",
            source = "bruteForceFailureResetTimeSeconds")
    @Mapping(target = "bruteForceMaxTemporaryLockouts", source = "bruteForceMaxTemporaryLockouts")
    @Mapping(target = "bruteForcePermanentLockout", source = "bruteForcePermanentLockout")
    @Mapping(target = "bruteForceIpRequestsPerMinute", source = "bruteForceIpRequestsPerMinute")
    @Mapping(
            target = "bruteForceUsernameIpRequestsPerMinute",
            source = "bruteForceUsernameIpRequestsPerMinute")
    @Mapping(target = "otpEnabled", source = "otpEnabled")
    @Mapping(target = "otpRequired", source = "otpRequired")
    @Mapping(target = "otpIssuer", source = "otpIssuer")
    @Mapping(target = "otpAlgorithm", source = "otpAlgorithm")
    @Mapping(target = "otpDigits", source = "otpDigits")
    @Mapping(target = "otpPeriodSeconds", source = "otpPeriodSeconds")
    @Mapping(target = "otpLookAheadWindow", source = "otpLookAheadWindow")
    @Mapping(target = "otpCodeReusable", source = "otpCodeReusable")
    @Mapping(target = "otpAddRecoveryCodes", source = "otpAddRecoveryCodes")
    @Mapping(target = "recoveryCodeWarningThreshold", source = "recoveryCodeWarningThreshold")
    AdminLoginSettingsDTO toAdminDTO(LoginSettingsEntity source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userRegistrationEnabled", source = "userRegistration")
    @Mapping(target = "forgotPasswordEnabled", source = "forgotPassword")
    @Mapping(target = "rememberMeEnabled", source = "rememberMe")
    @Mapping(target = "loginWithEmail", source = "loginWithEmail")
    @Mapping(target = "verifyEmail", source = "verifyEmail")
    @Mapping(target = "sessionTimeoutMinutes", source = "sessionTimeoutMinutes")
    @Mapping(target = "passwordMinimumLength", source = "passwordMinimumLength")
    @Mapping(target = "bruteForceEnabled", source = "bruteForceEnabled")
    @Mapping(target = "bruteForceMaxFailures", source = "bruteForceMaxFailures")
    @Mapping(target = "bruteForceMaxSecondaryFailures", source = "bruteForceMaxSecondaryFailures")
    @Mapping(target = "mfaVerificationTimeoutSeconds", source = "mfaVerificationTimeoutSeconds")
    @Mapping(target = "passwordMaximumLength", source = "passwordMaximumLength")
    @Mapping(target = "passwordMinimumUppercase", source = "passwordMinimumUppercase")
    @Mapping(target = "passwordMinimumLowercase", source = "passwordMinimumLowercase")
    @Mapping(target = "passwordMinimumDigits", source = "passwordMinimumDigits")
    @Mapping(
            target = "passwordMinimumSpecialCharacters",
            source = "passwordMinimumSpecialCharacters")
    @Mapping(target = "passwordRejectUsername", source = "passwordRejectUsername")
    @Mapping(target = "passwordRejectEmail", source = "passwordRejectEmail")
    @Mapping(target = "passwordRejectCommonPasswords", source = "passwordRejectCommonPasswords")
    @Mapping(target = "passwordHistorySize", source = "passwordHistorySize")
    @Mapping(target = "passwordExpirationDays", source = "passwordExpirationDays")
    @Mapping(target = "passwordCommonPasswords", source = "passwordCommonPasswords")
    @Mapping(
            target = "bruteForceQuickLoginWindowMillis",
            source = "bruteForceQuickLoginWindowMillis")
    @Mapping(
            target = "bruteForceMinimumQuickLoginWaitSeconds",
            source = "bruteForceMinimumQuickLoginWaitSeconds")
    @Mapping(target = "bruteForceWaitIncrementSeconds", source = "bruteForceWaitIncrementSeconds")
    @Mapping(target = "bruteForceMaxWaitSeconds", source = "bruteForceMaxWaitSeconds")
    @Mapping(
            target = "bruteForceFailureResetTimeSeconds",
            source = "bruteForceFailureResetTimeSeconds")
    @Mapping(target = "bruteForceMaxTemporaryLockouts", source = "bruteForceMaxTemporaryLockouts")
    @Mapping(target = "bruteForcePermanentLockout", source = "bruteForcePermanentLockout")
    @Mapping(target = "bruteForceIpRequestsPerMinute", source = "bruteForceIpRequestsPerMinute")
    @Mapping(
            target = "bruteForceUsernameIpRequestsPerMinute",
            source = "bruteForceUsernameIpRequestsPerMinute")
    @Mapping(target = "otpEnabled", source = "otpEnabled")
    @Mapping(target = "otpRequired", source = "otpRequired")
    @Mapping(target = "otpIssuer", source = "otpIssuer", qualifiedByName = "trim")
    @Mapping(
            target = "otpAlgorithm",
            source = "otpAlgorithm",
            qualifiedByName = "normalizeAlgorithm")
    @Mapping(target = "otpDigits", source = "otpDigits")
    @Mapping(target = "otpPeriodSeconds", source = "otpPeriodSeconds")
    @Mapping(target = "otpLookAheadWindow", source = "otpLookAheadWindow")
    @Mapping(target = "otpCodeReusable", source = "otpCodeReusable")
    @Mapping(target = "otpAddRecoveryCodes", source = "otpAddRecoveryCodes")
    @Mapping(target = "recoveryCodeWarningThreshold", source = "recoveryCodeWarningThreshold")
    void update(AdminLoginSettingsRequestDTO source, @MappingTarget LoginSettingsEntity target);

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Named("normalizeAlgorithm")
    default String normalizeAlgorithm(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
