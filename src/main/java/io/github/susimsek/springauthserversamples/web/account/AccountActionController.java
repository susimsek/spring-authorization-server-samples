package io.github.susimsek.springauthserversamples.web.account;

import io.github.susimsek.springauthserversamples.dto.account.ActionTokenRequestDTO;
import io.github.susimsek.springauthserversamples.dto.account.ForgotPasswordRequestDTO;
import io.github.susimsek.springauthserversamples.dto.account.ResetPasswordRequestDTO;
import io.github.susimsek.springauthserversamples.service.account.UserActionService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiController
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Account actions",
        description = "Public email verification and password reset actions.")
public class AccountActionController {

    private final UserActionService userActionService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        Locale locale = Locale.forLanguageTag(request.locale() == null ? "en" : request.locale());
        userActionService.forgotPassword(request.identifier(), locale);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify an email using a single-use token")
    ResponseEntity<Void> verifyEmail(@Valid @RequestBody ActionTokenRequestDTO request) {
        userActionService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a password using a single-use token")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        userActionService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
