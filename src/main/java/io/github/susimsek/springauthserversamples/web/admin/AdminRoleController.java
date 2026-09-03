package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRoleDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRoleDetailDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRoleRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRoleUserDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRoleUserRequestDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminRoleService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Admin - Roles", description = "Role and role membership administration.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    @Operation(summary = "Search roles")
    Page<AdminRoleDTO> roles(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminRoleService.roles(q, pageable);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get role")
    AdminRoleDetailDTO role(
            @PathVariable String name,
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminRoleService.role(name, q, pageable);
    }

    @GetMapping("/{name}/available-users")
    @Operation(summary = "Search users available for a role")
    Page<AdminRoleUserDTO> availableRoleUsers(
            @PathVariable String name,
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 10, sort = "username") Pageable pageable) {
        return adminRoleService.availableUsers(name, q, pageable);
    }

    @PostMapping("/{name}/users")
    @Operation(summary = "Assign user to role")
    AdminRoleDetailDTO assignRoleUser(
            @PathVariable String name,
            @Valid @RequestBody AdminRoleUserRequestDTO request,
            @PageableDefault(size = 20, sort = "username") Pageable pageable,
            Authentication authentication) {
        return adminRoleService.assignUser(
                name, request.userId(), authentication.getName(), pageable);
    }

    @DeleteMapping("/{name}/users/{userId}")
    @Operation(summary = "Remove user from role")
    AdminRoleDetailDTO removeRoleUser(
            @PathVariable String name,
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "username") Pageable pageable,
            Authentication authentication) {
        return adminRoleService.removeUser(name, userId, authentication.getName(), pageable);
    }

    @PostMapping
    @Operation(summary = "Create role")
    ResponseEntity<AdminRoleDTO> createRole(@Valid @RequestBody AdminRoleRequestDTO request) {
        return ResponseEntity.status(201).body(adminRoleService.createRole(request.name()));
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Delete role")
    ResponseEntity<Void> deleteRole(@PathVariable String name) {
        adminRoleService.deleteRole(name);
        return ResponseEntity.noContent().build();
    }
}
