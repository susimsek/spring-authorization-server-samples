package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.service.admin.AdminClientScopeService;
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
    Page<AdminClientScopeService.ClientScopeView> findAll(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminClientScopeService.findAll(q, pageable);
    }

    @PostMapping
    @Operation(summary = "Create client scope")
    ResponseEntity<AdminClientScopeService.ClientScopeView> create(
            @Valid @RequestBody AdminClientScopeRequest request) {
        var created = adminClientScopeService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/client-scopes/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update client scope")
    AdminClientScopeService.ClientScopeView update(
            @PathVariable String id, @Valid @RequestBody AdminClientScopeRequest request) {
        return adminClientScopeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete client scope")
    ResponseEntity<Void> delete(@PathVariable String id) {
        adminClientScopeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
