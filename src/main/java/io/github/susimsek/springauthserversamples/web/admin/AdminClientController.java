package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.service.admin.AdminClientService;
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
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
public class AdminClientController {

    private final AdminClientService adminClientService;

    @GetMapping
    Page<AdminClientView> findAll(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "clientId") Pageable pageable) {
        return adminClientService.findAll(q, pageable);
    }

    @GetMapping("/{id}")
    ResponseEntity<AdminClientView> findById(@PathVariable String id) {
        AdminClientView client = adminClientService.findById(id);
        return client == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(client);
    }

    @PostMapping
    ResponseEntity<AdminClientCreatedView> create(@Valid @RequestBody AdminClientRequest request) {
        AdminClientCreatedView created = adminClientService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/clients/" + created.client().id()))
                .body(created);
    }

    @PutMapping("/{id}")
    AdminClientView update(
            @PathVariable String id, @Valid @RequestBody AdminClientRequest request) {
        return adminClientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id) {
        adminClientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/secret")
    AdminClientSecretView regenerateSecret(@PathVariable String id) {
        return new AdminClientSecretView(adminClientService.regenerateSecret(id));
    }
}
