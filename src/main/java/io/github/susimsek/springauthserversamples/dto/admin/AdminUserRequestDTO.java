package io.github.susimsek.springauthserversamples.dto.admin;

import io.github.susimsek.springauthserversamples.web.admin.validation.CreateValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.OptionalPassword;
import io.github.susimsek.springauthserversamples.web.admin.validation.PasswordChangeValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.UpdateValidation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AdminUserRequestDTO(
        @NotBlank(
                        groups = {CreateValidation.class, UpdateValidation.class},
                        message = "{app.api.problem.violation.required}")
                @Size(
                        max = 100,
                        groups = {CreateValidation.class, UpdateValidation.class})
                String username,
        @Email(groups = {CreateValidation.class, UpdateValidation.class})
                @Size(
                        max = 200,
                        groups = {CreateValidation.class, UpdateValidation.class})
                String email,
        Boolean emailVerified,
        @NotBlank(
                        groups = {CreateValidation.class, PasswordChangeValidation.class},
                        message = "{app.api.problem.violation.required}")
                @Size(
                        min = 8,
                        max = 200,
                        groups = {CreateValidation.class, PasswordChangeValidation.class},
                        message = "{app.api.problem.violation.password}")
                @OptionalPassword(groups = UpdateValidation.class)
                String password,
        @NotNull(
                        groups = {CreateValidation.class, UpdateValidation.class},
                        message = "{app.api.problem.violation.required}")
                Boolean enabled,
        @NotEmpty(
                        groups = {CreateValidation.class, UpdateValidation.class},
                        message = "{app.api.problem.violation.roles}")
                Set<
                                @NotBlank(
                                        groups = {CreateValidation.class, UpdateValidation.class},
                                        message = "{app.api.problem.violation.roles}")
                                String>
                        roles) {

    public AdminUserRequestDTO(
            String username, String password, Boolean enabled, Set<String> roles) {
        this(username, null, false, password, enabled, roles);
    }
}
