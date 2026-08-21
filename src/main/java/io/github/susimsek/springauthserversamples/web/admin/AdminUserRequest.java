package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.web.admin.validation.CreateValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.OptionalPassword;
import io.github.susimsek.springauthserversamples.web.admin.validation.PasswordChangeValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.UpdateValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AdminUserRequest(
        @NotBlank(
                        groups = {CreateValidation.class, UpdateValidation.class},
                        message = "{admin.validation.required}")
                String username,
        @NotBlank(
                        groups = {CreateValidation.class, PasswordChangeValidation.class},
                        message = "{admin.validation.required}")
                @Size(
                        min = 8,
                        groups = {CreateValidation.class, PasswordChangeValidation.class},
                        message = "{admin.validation.password}")
                @OptionalPassword(groups = UpdateValidation.class)
                String password,
        @NotNull(
                        groups = {CreateValidation.class, UpdateValidation.class},
                        message = "{admin.validation.required}")
                Boolean enabled,
        @NotEmpty(
                        groups = {CreateValidation.class, UpdateValidation.class},
                        message = "{admin.validation.roles}")
                Set<
                                @NotBlank(
                                        groups = {CreateValidation.class, UpdateValidation.class},
                                        message = "{admin.validation.roles}")
                                String>
                        roles) {}
