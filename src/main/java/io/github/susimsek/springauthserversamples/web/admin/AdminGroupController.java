package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRolesRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupUserDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupUserRequestDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminGroupService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiController
@RequestMapping("/api/admin/groups")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Groups",
        description = "Keycloak-style groups, memberships, and realm-role mappings.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
public class AdminGroupController {

    private final AdminGroupService adminGroupService;

    @GetMapping
    @Operation(
            summary = "Search groups",
            description = "Returns a paged group list. `size` is capped at 100.")
    Page<AdminGroupDTO> findAll(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminGroupService.findAll(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group")
    AdminGroupDTO findById(@PathVariable Long id) {
        return adminGroupService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create group")
    ResponseEntity<AdminGroupDTO> create(@Valid @RequestBody AdminGroupRequestDTO request) {
        AdminGroupDTO created = adminGroupService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/groups/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename group")
    AdminGroupDTO update(@PathVariable Long id, @Valid @RequestBody AdminGroupRequestDTO request) {
        return adminGroupService.update(id, request);
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Replace group role mappings")
    AdminGroupDTO updateRoles(
            @PathVariable Long id, @Valid @RequestBody AdminGroupRolesRequestDTO request) {
        return adminGroupService.updateRoles(id, request);
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "List group members")
    Page<AdminGroupUserDTO> users(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminGroupService.users(id, q, pageable);
    }

    @GetMapping("/{id}/available-users")
    @Operation(summary = "Search users that are not group members")
    Page<AdminGroupUserDTO> availableUsers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 10, sort = "username") Pageable pageable) {
        return adminGroupService.availableUsers(id, q, pageable);
    }

    @PostMapping("/{id}/users")
    @Operation(summary = "Add user to group")
    AdminGroupDTO addUser(
            @PathVariable Long id, @Valid @RequestBody AdminGroupUserRequestDTO request) {
        return adminGroupService.addUser(id, request.userId());
    }

    @DeleteMapping("/{id}/users/{userId}")
    @Operation(summary = "Remove user from group")
    AdminGroupDTO removeUser(@PathVariable Long id, @PathVariable Long userId) {
        return adminGroupService.removeUser(id, userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete group")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        adminGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
