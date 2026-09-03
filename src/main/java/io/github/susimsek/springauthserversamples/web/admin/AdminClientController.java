package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminClientCreatedDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminClientDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminClientRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminClientScopeAssignmentRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminClientSecretDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminConsentDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEventDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminScopeAssignmentsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminSessionDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.AdminClientScopeService;
import io.github.susimsek.springauthserversamples.service.admin.AdminClientService;
import io.github.susimsek.springauthserversamples.service.admin.AdminConsentService;
import io.github.susimsek.springauthserversamples.service.admin.AdminSessionService;
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
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Clients",
        description = "Keycloak-style registered OAuth2/OIDC client management.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
public class AdminClientController {

    private final AdminClientService adminClientService;
    private final AdminClientScopeService adminClientScopeService;
    private final AdminSessionService adminSessionService;
    private final AdminConsentService adminConsentService;
    private final AdminAuditEventService adminAuditEventService;

    @GetMapping
    @Operation(
            summary = "Search clients",
            description = "Returns a paged client list. `size` is capped at 100.")
    Page<AdminClientDTO> findAll(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "clientId") Pageable pageable) {
        return adminClientService.findAll(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get client",
            description = "Returns one registered client by its internal identifier.")
    ResponseEntity<AdminClientDTO> findById(@PathVariable String id) {
        AdminClientDTO client = adminClientService.findById(id);
        return client == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(client);
    }

    @PostMapping
    @Operation(summary = "Create client", description = "Creates a registered OAuth2/OIDC client.")
    ResponseEntity<AdminClientCreatedDTO> create(
            @Valid @RequestBody AdminClientRequestDTO request) {
        AdminClientCreatedDTO created = adminClientService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/clients/" + created.client().id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update client", description = "Updates a registered OAuth2/OIDC client.")
    AdminClientDTO update(
            @PathVariable String id, @Valid @RequestBody AdminClientRequestDTO request) {
        return adminClientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete client",
            description = "Deletes a registered client and its assignments.")
    ResponseEntity<Void> delete(@PathVariable String id) {
        adminClientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/secret")
    @Operation(
            summary = "Regenerate client secret",
            description = "Returns the replacement client secret once.")
    AdminClientSecretDTO regenerateSecret(@PathVariable String id) {
        return new AdminClientSecretDTO(adminClientService.regenerateSecret(id));
    }

    @GetMapping("/{id}/scope-assignments")
    @Operation(summary = "Get client scope assignments")
    AdminScopeAssignmentsDTO scopeAssignments(@PathVariable String id) {
        return adminClientScopeService.assignments(id);
    }

    @PutMapping("/{id}/scope-assignments")
    @Operation(summary = "Update client scope assignments")
    AdminScopeAssignmentsDTO updateScopeAssignments(
            @PathVariable String id,
            @Valid @RequestBody AdminClientScopeAssignmentRequestDTO request) {
        return adminClientScopeService.updateAssignments(id, request);
    }

    @GetMapping("/{id}/sessions")
    @Operation(
            summary = "List client sessions",
            description = "Returns browser sessions associated with the client.")
    Page<AdminSessionDTO> sessions(
            @PathVariable String id,
            @PageableDefault(
                            size = 20,
                            sort = "lastAccessTime",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return adminSessionService.clientSessions(id, pageable);
    }

    @GetMapping("/{id}/consents")
    @Operation(
            summary = "List client consents",
            description = "Returns user consents granted to the client.")
    Page<AdminConsentDTO> consents(
            @PathVariable String id,
            @PageableDefault(size = 20, sort = "id.principalName") Pageable pageable) {
        return adminConsentService.clientConsents(id, pageable);
    }

    @GetMapping("/{id}/events")
    @Operation(
            summary = "List client events",
            description = "Returns administrative audit events for the client.")
    Page<AdminEventDTO> events(
            @PathVariable String id,
            @PageableDefault(
                            size = 20,
                            sort = "occurredAt",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        if (adminClientService.findById(id) == null) {
            throw io.github.susimsek.springauthserversamples.service.error.ApiException.notFound(
                    "Client not found");
        }
        return adminAuditEventService.clientEvents(id, pageable);
    }
}
