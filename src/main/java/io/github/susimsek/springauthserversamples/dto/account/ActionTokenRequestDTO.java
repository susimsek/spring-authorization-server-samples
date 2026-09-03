package io.github.susimsek.springauthserversamples.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActionTokenRequestDTO(@NotBlank @Size(max = 200) String token) {}
