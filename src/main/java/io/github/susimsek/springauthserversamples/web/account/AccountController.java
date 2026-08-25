package io.github.susimsek.springauthserversamples.web.account;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.account.AccountApplicationDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountPasswordRequestDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileRequestDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountSessionDTO;
import io.github.susimsek.springauthserversamples.service.account.AccountService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@ApiController
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(
        name = "Account",
        description = "Account Console profile, session, and application management.")
@SecurityRequirement(name = OpenApiConfig.ACCOUNT_BEARER)
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/profile")
    @Operation(summary = "Read profile")
    AccountProfileDTO profile(Authentication authentication) {
        return accountService.profile(authentication.getName());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile")
    AccountProfileDTO updateProfile(
            Authentication authentication, @Valid @RequestBody AccountProfileRequestDTO request) {
        return accountService.updateProfile(authentication.getName(), request);
    }

    @PutMapping("/password")
    @Operation(summary = "Change password")
    ResponseEntity<Void> changePassword(
            Authentication authentication, @Valid @RequestBody AccountPasswordRequestDTO request) {
        accountService.changePassword(
                authentication.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    @Operation(
            summary = "List sessions",
            description =
                    "Returns the authenticated account's active sessions. `size` is capped at 100.")
    Page<AccountSessionDTO> sessions(
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(
                            size = 20,
                            sort = "lastAccessTime",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return accountService.sessions(
                authentication.getName(), jwt.getClaimAsString("sid"), pageable);
    }

    @DeleteMapping("/sessions/others")
    @Operation(summary = "Sign out other sessions")
    ResponseEntity<Void> deleteOtherSessions(
            Authentication authentication, @AuthenticationPrincipal Jwt jwt) {
        accountService.deleteOtherSessions(authentication.getName(), jwt.getClaimAsString("sid"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions")
    @Operation(summary = "Sign out all sessions")
    ResponseEntity<Void> deleteAllSessions(Authentication authentication) {
        accountService.deleteAllSessions(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Sign out a session")
    ResponseEntity<Void> deleteSession(
            Authentication authentication, @PathVariable String sessionId) {
        accountService.deleteSession(authentication.getName(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/applications")
    @Operation(
            summary = "List authorized applications",
            description =
                    "Returns a paged list of OAuth2/OIDC clients authorized by the account. `size`"
                            + " is capped at 100.")
    Page<AccountApplicationDTO> applications(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "id.registeredClientId") Pageable pageable) {
        return accountService.applications(authentication.getName(), pageable);
    }

    @DeleteMapping("/applications/{clientId}")
    @Operation(summary = "Revoke application consent")
    ResponseEntity<Void> revokeApplication(
            Authentication authentication, @PathVariable String clientId) {
        accountService.revokeApplication(authentication.getName(), clientId);
        return ResponseEntity.noContent().build();
    }
}
