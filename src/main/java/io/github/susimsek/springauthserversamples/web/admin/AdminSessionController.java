package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSessionDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSessionDetailDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminSessionService;
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
@Tag(name = "Admin - Sessions", description = "Browser session administration.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
class AdminSessionController {

    private final AdminSessionService adminSessionService;

    @GetMapping("/users/{id}/sessions")
    @Operation(summary = "List user sessions")
    Page<AdminSessionDTO> userSessions(
            @PathVariable Long id,
            @PageableDefault(
                            size = 20,
                            sort = "lastAccessTime",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable,
            Authentication authentication) {
        return adminSessionService.userSessions(id, authentication.getName(), pageable);
    }

    @GetMapping("/sessions")
    @Operation(summary = "Search browser sessions")
    Page<AdminSessionDTO> sessions(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String clientId,
            @RequestParam(defaultValue = "active") String status,
            @PageableDefault(
                            size = 20,
                            sort = "lastAccessTime",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return adminSessionService.sessions(q, clientId, status, pageable);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get browser session")
    AdminSessionDetailDTO session(@PathVariable String id, Authentication authentication) {
        return adminSessionService.session(id, authentication.getName());
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Delete browser session")
    ResponseEntity<Void> deleteSession(@PathVariable String id, Authentication authentication) {
        adminSessionService.deleteSession(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{username}/sessions")
    @Operation(summary = "Delete all sessions for a user")
    ResponseEntity<Void> deleteUserSessions(
            @PathVariable String username, Authentication authentication) {
        adminSessionService.deleteUserSessions(username, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
