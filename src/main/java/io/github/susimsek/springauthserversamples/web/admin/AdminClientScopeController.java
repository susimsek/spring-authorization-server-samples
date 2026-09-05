package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminClientScopeDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminClientScopeRequestDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminClientScopeService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/api/admin/client-scopes")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Client Scopes",
        description = "Keycloak-style client scope catalogue management.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
public class AdminClientScopeController {

    private final AdminClientScopeService adminClientScopeService;

    @GetMapping
    @Operation(
            summary = "Search client scopes",
            description = "Returns a paged client-scope list. `size` is capped at 100.")
    @ApiResponse(responseCode = "200", description = "Paged client scopes returned.")
    Page<AdminClientScopeDTO> findAll(
            @Parameter(
                            description = "Optional scope name or display-name search text.",
                            example = "account")
                    @RequestParam(defaultValue = "")
                    String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminClientScopeService.findAll(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get client scope",
            description = "Returns a single client scope from the scope catalogue.")
    @ApiResponse(responseCode = "200", description = "Client scope returned.")
    AdminClientScopeDTO findOne(
            @Parameter(
                            description = "Internal scope identifier.",
                            example = "scope-123",
                            required = true)
                    @PathVariable
                    String id) {
        return adminClientScopeService.findOne(id);
    }

    @PostMapping
    @Operation(
            summary = "Create client scope",
            description = "Creates a client scope in the scope catalogue.")
    @ApiResponse(responseCode = "201", description = "Client scope created and returned.")
    ResponseEntity<AdminClientScopeDTO> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "Client scope definition.",
                            required = true)
                    @Valid
                    @RequestBody
                    AdminClientScopeRequestDTO request) {
        var created = adminClientScopeService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/client-scopes/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update client scope",
            description = "Updates a client scope in the scope catalogue.")
    @ApiResponse(responseCode = "200", description = "Updated client scope returned.")
    AdminClientScopeDTO update(
            @Parameter(
                            description = "Internal scope identifier.",
                            example = "scope-123",
                            required = true)
                    @PathVariable
                    String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "Replacement client scope definition.",
                            required = true)
                    @Valid
                    @RequestBody
                    AdminClientScopeRequestDTO request) {
        return adminClientScopeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete client scope",
            description = "Deletes a client scope that is no longer assigned to clients.")
    @ApiResponse(responseCode = "204", description = "Client scope deleted.")
    ResponseEntity<Void> delete(
            @Parameter(
                            description = "Internal scope identifier.",
                            example = "scope-123",
                            required = true)
                    @PathVariable
                    String id) {
        adminClientScopeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
