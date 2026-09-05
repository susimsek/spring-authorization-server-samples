package io.github.susimsek.springauthserversamples.service.requiredaction;

import io.github.susimsek.springauthserversamples.domain.RequiredActionDefinitionEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileRequestDTO;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.service.security.PasswordPolicyService;
import io.github.susimsek.springauthserversamples.service.security.PasswordService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
final class StandardRequiredActionHandler implements RequiredActionHandler {

    private final Validator validator;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordService passwordService;
    private final UserAccessInvalidationService userAccessInvalidationService;

    @Autowired
    StandardRequiredActionHandler(
            Validator validator,
            PasswordPolicyService passwordPolicyService,
            PasswordService passwordService,
            UserAccessInvalidationService userAccessInvalidationService) {
        this.validator = validator;
        this.passwordPolicyService = passwordPolicyService;
        this.passwordService = passwordService;
        this.userAccessInvalidationService = userAccessInvalidationService;
    }

    StandardRequiredActionHandler(Validator validator) {
        this.validator = validator;
        this.passwordPolicyService = null;
        this.passwordService = null;
        this.userAccessInvalidationService = null;
    }

    @Override
    public String key() {
        return "*";
    }

    @Override
    public boolean isPending(
            UserEntity user, RequiredActionDefinitionEntity definition, boolean completed) {
        return switch (definition.getActionKey()) {
            case "UPDATE_PROFILE" ->
                    isBlank(user.getFirstName())
                            || isBlank(user.getLastName())
                            || isBlank(user.getEmail());
            case "UPDATE_EMAIL" -> user.getPendingEmail() != null;
            case "UPDATE_PASSWORD" ->
                    user.isMustChangePassword()
                            || user.isTemporaryPassword()
                            || (passwordPolicyService != null
                                    && passwordPolicyService.isExpired(user));
            default -> !completed;
        };
    }

    @Override
    public void complete(UserEntity user, Map<String, Object> values) {
        throw ApiException.badRequest(
                ApiErrorCode.ACTION_UNSUPPORTED,
                "A handler must be registered for this required action");
    }

    void completeStandard(UserEntity user, String key, Map<String, Object> values) {
        Map<String, Object> submitted = values == null ? Map.of() : values;
        switch (key) {
            case "UPDATE_PROFILE" -> {
                String firstName = value(submitted, "firstName");
                String lastName = value(submitted, "lastName");
                String email = value(submitted, "email");
                if (isBlank(firstName) || isBlank(lastName) || isBlank(email)) {
                    throw ApiException.badRequest(
                            ApiErrorCode.INVALID_REQUEST,
                            "First name, last name, and email are required");
                }
                AccountProfileRequestDTO profile =
                        new AccountProfileRequestDTO(firstName, lastName, email);
                for (ConstraintViolation<AccountProfileRequestDTO> violation :
                        validator.validate(profile)) {
                    throw ApiException.badRequest(
                            violation.getPropertyPath().toString(),
                            ApiErrorCode.INVALID_REQUEST,
                            "Profile value is invalid");
                }
                user.setFirstName(firstName.trim());
                user.setLastName(lastName.trim());
                user.setEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
                user.setEmailVerified(false);
            }
            case "TERMS_AND_CONDITIONS", "DELETE_ACCOUNT", "CUSTOM" -> {
                if (!Boolean.TRUE.equals(submitted.get("accepted"))
                        && !Boolean.TRUE.equals(submitted.get("confirmed"))) {
                    throw ApiException.badRequest(
                            ApiErrorCode.INVALID_REQUEST,
                            "The required action must be explicitly confirmed");
                }
            }
            case "UPDATE_EMAIL" -> {
                if (user.getPendingEmail() == null) {
                    throw ApiException.badRequest(
                            ApiErrorCode.ACTION_UNSUPPORTED, "There is no pending email change");
                }
            }
            case "UPDATE_PASSWORD" -> {
                String newPassword = value(submitted, "newPassword");
                if (passwordService == null) {
                    throw ApiException.badRequest(
                            ApiErrorCode.ACTION_UNSUPPORTED, "Password service is unavailable");
                }
                passwordService.changePassword(user, newPassword);
                if (userAccessInvalidationService != null) {
                    userAccessInvalidationService.invalidate(user.getUsername());
                }
            }
            default ->
                    throw ApiException.badRequest(
                            ApiErrorCode.ACTION_UNSUPPORTED,
                            "The required action is not supported");
        }
    }

    private static String value(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : Objects.toString(value, null);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
