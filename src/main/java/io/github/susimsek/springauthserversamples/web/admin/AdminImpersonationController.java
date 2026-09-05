package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminImpersonationDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminUserService;
import io.github.susimsek.springauthserversamples.service.admin.ImpersonationService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiController
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Impersonation", description = "Controlled user impersonation handoffs.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
public class AdminImpersonationController {

    private final AdminUserService adminUserService;
    private final ImpersonationService impersonationService;

    @PostMapping("/{id}/impersonation")
    @Operation(
            summary = "Create an impersonation handoff",
            description =
                    "Creates a short-lived browser handoff that allows an administrator to view"
                            + " the selected user's account console.")
    @ApiResponse(responseCode = "200", description = "Impersonation handoff created.")
    AdminImpersonationDTO impersonate(
            @Parameter(
                            description = "Internal target user identifier.",
                            example = "2",
                            required = true)
                    @PathVariable
                    Long id,
            Authentication authentication,
            HttpServletResponse response) {
        adminUserService.requireManageableUser(id, authentication.getName());
        AdminImpersonationDTO result = impersonationService.issue(id, authentication.getName());
        response.addHeader(
                "Set-Cookie",
                ResponseCookie.from("IMPERSONATION_TICKET", result.ticket())
                        .httpOnly(true)
                        .sameSite("Lax")
                        .path("/impersonation")
                        .maxAge(java.time.Duration.ofSeconds(60))
                        .build()
                        .toString());
        return result;
    }
}
