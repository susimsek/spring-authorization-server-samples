package io.github.susimsek.springauthserversamples.web.admin.validation;

import io.github.susimsek.springauthserversamples.web.admin.AdminClientRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

public class AdminClientConfigurationValidator
        implements ConstraintValidator<ValidAdminClientConfiguration, AdminClientRequest> {

    private static final Set<String> ALLOWED_METHODS =
            Set.of(
                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(),
                    ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue(),
                    ClientAuthenticationMethod.NONE.getValue());
    private static final Set<String> ALLOWED_GRANTS =
            Set.of(
                    AuthorizationGrantType.AUTHORIZATION_CODE.getValue(),
                    AuthorizationGrantType.REFRESH_TOKEN.getValue(),
                    AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());

    @Override
    public boolean isValid(AdminClientRequest request, ConstraintValidatorContext context) {
        if (request == null
                || request.clientAuthenticationMethods() == null
                || request.authorizationGrantTypes() == null) {
            return true;
        }

        Set<String> methods = request.clientAuthenticationMethods();
        Set<String> grants = request.authorizationGrantTypes();
        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (!ALLOWED_METHODS.containsAll(methods)
                || (methods.contains(ClientAuthenticationMethod.NONE.getValue())
                        && methods.size() > 1)) {
            valid = false;
            violation(context, "clientAuthenticationMethods", "{admin.validation.selection}");
        }
        if (!ALLOWED_GRANTS.containsAll(grants)
                || (methods.contains(ClientAuthenticationMethod.NONE.getValue())
                        && grants.contains(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()))) {
            valid = false;
            violation(context, "authorizationGrantTypes", "{admin.validation.selection}");
        }

        boolean authorizationCode =
                grants.contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        if (authorizationCode
                && (request.redirectUris() == null || request.redirectUris().isEmpty())) {
            valid = false;
            violation(context, "redirectUris", "{admin.validation.required}");
        }
        if (methods.contains(ClientAuthenticationMethod.NONE.getValue())
                && authorizationCode
                && !request.requireProofKey()) {
            valid = false;
            violation(context, "authorizationGrantTypes", "{admin.validation.selection}");
        }
        if (request.requireProofKey() && !authorizationCode) {
            valid = false;
            violation(context, "authorizationGrantTypes", "{admin.validation.selection}");
        }
        return valid;
    }

    private static void violation(
            ConstraintValidatorContext context, String field, String messageTemplate) {
        context.buildConstraintViolationWithTemplate(messageTemplate)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
