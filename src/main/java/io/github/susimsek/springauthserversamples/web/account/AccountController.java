package io.github.susimsek.springauthserversamples.web.account;

import io.github.susimsek.springauthserversamples.service.account.AccountService;
import io.github.susimsek.springauthserversamples.web.admin.AccountApi;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AccountApi
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/profile")
    AccountService.ProfileView profile(Authentication authentication) {
        return accountService.profile(authentication.getName());
    }

    @PutMapping("/profile")
    AccountService.ProfileView updateProfile(
            Authentication authentication, @Valid @RequestBody AccountProfileRequest request) {
        return accountService.updateProfile(
                authentication.getName(), request.firstName(), request.lastName(), request.email());
    }

    @PutMapping("/password")
    ResponseEntity<Void> changePassword(
            Authentication authentication, @Valid @RequestBody AccountPasswordRequest request) {
        accountService.changePassword(
                authentication.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    List<AccountService.SessionView> sessions(
            Authentication authentication, @AuthenticationPrincipal Jwt jwt) {
        return accountService.sessions(authentication.getName(), jwt.getClaimAsString("sid"));
    }

    @DeleteMapping("/sessions/others")
    ResponseEntity<Void> deleteOtherSessions(
            Authentication authentication, @AuthenticationPrincipal Jwt jwt) {
        accountService.deleteOtherSessions(authentication.getName(), jwt.getClaimAsString("sid"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions")
    ResponseEntity<Void> deleteAllSessions(Authentication authentication) {
        accountService.deleteAllSessions(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions/{sessionId}")
    ResponseEntity<Void> deleteSession(
            Authentication authentication, @PathVariable String sessionId) {
        accountService.deleteSession(authentication.getName(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/applications")
    List<AccountService.ApplicationView> applications(Authentication authentication) {
        return accountService.applications(authentication.getName());
    }

    @DeleteMapping("/applications/{clientId}")
    ResponseEntity<Void> revokeApplication(
            Authentication authentication, @PathVariable String clientId) {
        accountService.revokeApplication(authentication.getName(), clientId);
        return ResponseEntity.noContent().build();
    }
}
