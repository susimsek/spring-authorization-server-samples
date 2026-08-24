package io.github.susimsek.springauthserversamples.web.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountPasswordRequest(
        @NotBlank @Size(max = 200) String currentPassword,
        @NotBlank @Size(min = 8, max = 200) String newPassword) {}
