package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEmailSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEmailSettingsRequestDTO;
import io.github.susimsek.springauthserversamples.service.EmailSettingsService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiController
@RestController
@RequestMapping("/api/admin/settings/email")
@RequiredArgsConstructor
@Tag(name = "Admin - Email settings", description = "SMTP configuration.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
class AdminEmailSettingsController {
    private final EmailSettingsService service;

    @GetMapping
    @Operation(
            summary = "Get email settings",
            description = "Returns the SMTP configuration without exposing the stored password.")
    @ApiResponse(responseCode = "200", description = "Email settings returned.")
    AdminEmailSettingsDTO get() {
        return service.get();
    }

    @PutMapping
    @Operation(
            summary = "Update email settings",
            description =
                    "Updates SMTP delivery settings; a blank password retains the existing secret.")
    @ApiResponse(responseCode = "200", description = "Email settings updated.")
    AdminEmailSettingsDTO update(@Valid @RequestBody AdminEmailSettingsRequestDTO request) {
        return service.update(request);
    }
}
