package io.github.susimsek.springauthserversamples.web.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record AccountProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Email @Size(max = 200) String email) {}
