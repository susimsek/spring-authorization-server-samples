package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminConsentDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminConsentService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Consents", description = "OAuth2 consent administration.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
class AdminConsentController {

    private final AdminConsentService adminConsentService;

    @GetMapping("/users/{id}/consents")
    @Operation(summary = "List user consents")
    Page<AdminConsentDTO> userConsents(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "id.registeredClientId") Pageable pageable,
            Authentication authentication) {
        return adminConsentService.userConsents(id, authentication.getName(), pageable);
    }

    @GetMapping("/consents")
    @Operation(summary = "Search user consents")
    Page<AdminConsentDTO> consents(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String clientId,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String scope,
            @PageableDefault(size = 20, sort = "id.principalName") Pageable pageable) {
        return adminConsentService.consents(q, clientId, username, scope, pageable);
    }

    @GetMapping("/consents/{clientId}/{username}")
    @Operation(summary = "Get user consent")
    AdminConsentDTO consent(@PathVariable String clientId, @PathVariable String username) {
        return adminConsentService.consent(clientId, username);
    }

    @DeleteMapping("/consents/{clientId}/{username}")
    @Operation(summary = "Revoke user consent")
    ResponseEntity<Void> revokeConsent(
            @PathVariable String clientId,
            @PathVariable String username,
            Authentication authentication) {
        adminConsentService.revokeConsent(clientId, username, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
