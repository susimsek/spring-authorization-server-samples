package io.github.susimsek.springauthserversamples.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(min = 8, max = 200) String newPassword) {}
