package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminKeyDTO;
import io.github.susimsek.springauthserversamples.service.admin.KeyManagementService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiController
@RequestMapping("/api/admin/keys")
@RequiredArgsConstructor
@Tag(name = "Admin - Keys", description = "Signing key administration.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
class AdminKeyController {

    private final KeyManagementService keyManagementService;

    @GetMapping
    @Operation(summary = "List signing keys")
    Page<AdminKeyDTO> keys(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(
                            size = 20,
                            sort = "createdAt",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return keyManagementService.keys(q, active, pageable);
    }

    @PostMapping("/rotate")
    @Operation(summary = "Rotate signing key")
    AdminKeyDTO rotateKey() {
        return keyManagementService.rotateKey();
    }
}
