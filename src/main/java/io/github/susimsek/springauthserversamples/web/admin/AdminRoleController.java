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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(summary = "Search roles", description = "Returns a paged list of realm roles.")
    @ApiResponse(responseCode = "200", description = "Paged roles returned.")
    Page<AdminRoleDTO> roles(
            @Parameter(description = "Optional role-name search text.", example = "USER")
                    @RequestParam(defaultValue = "")
                    String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminRoleService.roles(q, pageable);
    }

    @GetMapping("/{name}")
    @Operation(
            summary = "Get role",
            description = "Returns role details and its paged user membership.")
    @ApiResponse(responseCode = "200", description = "Role details returned.")
    AdminRoleDetailDTO role(
            @Parameter(
                            description = "Realm role name.",
                            example = "ROLE_USER_VIEWER",
                            required = true)
                    @PathVariable
                    String name,
            @Parameter(description = "Optional username search text.", example = "user")
                    @RequestParam(defaultValue = "")
                    String q,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminRoleService.role(name, q, pageable);
    }

    @GetMapping("/{name}/available-users")
    @Operation(
            summary = "Search users available for a role",
            description = "Returns enabled users who are not currently assigned to the role.")
    @ApiResponse(responseCode = "200", description = "Paged eligible users returned.")
    Page<AdminRoleUserDTO> availableRoleUsers(
            @Parameter(
                            description = "Realm role name.",
                            example = "ROLE_USER_VIEWER",
                            required = true)
                    @PathVariable
                    String name,
            @Parameter(description = "Optional username search text.", example = "user")
                    @RequestParam(defaultValue = "")
                    String q,
            @PageableDefault(size = 10, sort = "username") Pageable pageable) {
        return adminRoleService.availableUsers(name, q, pageable);
    }

    @PostMapping("/{name}/users")
    @Operation(
            summary = "Assign user to role",
            description = "Assigns a user to the role and returns the updated role details.")
    @ApiResponse(responseCode = "200", description = "Updated role details returned.")
    AdminRoleDetailDTO assignRoleUser(
            @Parameter(
                            description = "Realm role name.",
                            example = "ROLE_USER_VIEWER",
                            required = true)
                    @PathVariable
                    String name,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "User to assign.",
                            required = true)
                    @Valid
                    @RequestBody
                    AdminRoleUserRequestDTO request,
            @PageableDefault(size = 20, sort = "username") Pageable pageable,
            Authentication authentication) {
        return adminRoleService.assignUser(
                name, request.userId(), authentication.getName(), pageable);
    }

    @DeleteMapping("/{name}/users/{userId}")
    @Operation(
            summary = "Remove user from role",
            description = "Removes a user from the role and returns the updated role details.")
    @ApiResponse(responseCode = "200", description = "Updated role details returned.")
    AdminRoleDetailDTO removeRoleUser(
            @Parameter(
                            description = "Realm role name.",
                            example = "ROLE_USER_VIEWER",
                            required = true)
                    @PathVariable
                    String name,
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long userId,
            @PageableDefault(size = 20, sort = "username") Pageable pageable,
            Authentication authentication) {
        return adminRoleService.removeUser(name, userId, authentication.getName(), pageable);
    }

    @PostMapping
    @Operation(summary = "Create role", description = "Creates a new realm role.")
    @ApiResponse(responseCode = "201", description = "Role created and returned.")
    ResponseEntity<AdminRoleDTO> createRole(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "Role name.",
                            required = true)
                    @Valid
                    @RequestBody
                    AdminRoleRequestDTO request) {
        return ResponseEntity.status(201).body(adminRoleService.createRole(request.name()));
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Delete role", description = "Deletes a non-protected realm role.")
    @ApiResponse(responseCode = "204", description = "Role deleted.")
    ResponseEntity<Void> deleteRole(
            @Parameter(description = "Realm role name.", example = "ROLE_AUDITOR", required = true)
                    @PathVariable
                    String name) {
        adminRoleService.deleteRole(name);
        return ResponseEntity.noContent().build();
    }
}
