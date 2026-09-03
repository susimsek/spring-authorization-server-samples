package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminDashboardDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminServerInfoDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminDashboardService;
import io.github.susimsek.springauthserversamples.service.admin.AdminServerInfoService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Dashboard", description = "Administration dashboard and server metadata.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminServerInfoService adminServerInfoService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get administration dashboard")
    AdminDashboardDTO dashboard() {
        return adminDashboardService.dashboard();
    }

    @GetMapping("/server-info")
    @Operation(summary = "Get server information")
    AdminServerInfoDTO serverInfo() {
        return adminServerInfoService.serverInfo();
    }
}
