package io.github.susimsek.springauthserversamples.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequestDTO(
        @NotBlank @Size(max = 200) String identifier, @Size(max = 10) String locale) {}
