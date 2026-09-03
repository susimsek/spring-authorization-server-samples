package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEventDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.AdminUserService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Events", description = "Administrative audit events.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
class AdminEventController {

    private final AdminAuditEventService adminAuditEventService;
    private final AdminUserService adminUserService;

    @GetMapping("/events")
    @Operation(summary = "Search administrative events")
    Page<AdminEventDTO> events(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String targetType,
            @RequestParam(defaultValue = "") String targetId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(
                            size = 20,
                            sort = "occurredAt",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return adminAuditEventService.events(q, action, targetType, targetId, from, to, pageable);
    }

    @GetMapping("/users/{id}/events")
    @Operation(summary = "List user events")
    Page<AdminEventDTO> userEvents(
            @PathVariable Long id,
            @PageableDefault(
                            size = 20,
                            sort = "occurredAt",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable,
            Authentication authentication) {
        adminUserService.requireManageableUser(id, authentication.getName());
        return adminAuditEventService.userEvents(id, pageable);
    }
}
