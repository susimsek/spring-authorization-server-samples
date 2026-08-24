package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.service.admin.AdminClientScopeService;
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
@AdminApi
@RequestMapping("/api/admin/client-scopes")
@RequiredArgsConstructor
public class AdminClientScopeController {

    private final AdminClientScopeService adminClientScopeService;

    @GetMapping
    Page<AdminClientScopeService.ClientScopeView> findAll(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminClientScopeService.findAll(q, pageable);
    }

    @PostMapping
    ResponseEntity<AdminClientScopeService.ClientScopeView> create(
            @Valid @RequestBody AdminClientScopeRequest request) {
        var created = adminClientScopeService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/client-scopes/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    AdminClientScopeService.ClientScopeView update(
            @PathVariable String id, @Valid @RequestBody AdminClientScopeRequest request) {
        return adminClientScopeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id) {
        adminClientScopeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
