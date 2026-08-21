package io.github.susimsek.springauthserversamples.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.service.admin.AdminClientService;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

class AdminClientControllerTest {

    private final AdminClientService adminClientService = mock(AdminClientService.class);
    private final AdminClientController controller = new AdminClientController(adminClientService);

    @Test
    void returnsClientPage() {
        PageRequest pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(java.util.List.of(clientView()));
        when(adminClientService.findAll("demo", pageable)).thenReturn(page);

        var result = controller.findAll("demo", pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void returnsClientWhenFound() {
        AdminClientView client = clientView();
        when(adminClientService.findById("client-1")).thenReturn(client);

        var response = controller.findById("client-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(client);
    }

    @Test
    void returnsNotFoundWhenClientMissing() {
        when(adminClientService.findById("missing")).thenReturn(null);

        var response = controller.findById("missing");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void createsClientWithLocationHeader() {
        AdminClientRequest request = clientRequest();
        AdminClientCreatedView created = new AdminClientCreatedView(clientView(), "secret-1");
        when(adminClientService.create(request)).thenReturn(created);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/admin/clients/client-1");
        assertThat(response.getBody()).isSameAs(created);
    }

    @Test
    void updatesClient() {
        AdminClientRequest request = clientRequest();
        AdminClientView client = clientView();
        when(adminClientService.update("client-1", request)).thenReturn(client);

        var response = controller.update("client-1", request);

        assertThat(response).isSameAs(client);
    }

    @Test
    void deletesClient() {
        var response = controller.delete("client-1");

        verify(adminClientService).delete("client-1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void regeneratesClientSecret() {
        when(adminClientService.regenerateSecret("client-1")).thenReturn("new-secret");

        var response = controller.regenerateSecret("client-1");

        assertThat(response.clientSecret()).isEqualTo("new-secret");
    }

    private static AdminClientRequest clientRequest() {
        return new AdminClientRequest(
                "demo-client",
                "Demo Client",
                Set.of("client_secret_basic"),
                Set.of("client_credentials"),
                Set.of(),
                Set.of(),
                Set.of("openid"),
                false,
                false,
                Duration.ofMinutes(5),
                Duration.ofMinutes(5),
                Duration.ofHours(1));
    }

    private static AdminClientView clientView() {
        return new AdminClientView(
                "client-1",
                "demo-client",
                "Demo Client",
                Instant.parse("2026-08-21T00:00:00Z"),
                null,
                Set.of("client_secret_basic"),
                Set.of("client_credentials"),
                Set.of(),
                Set.of(),
                Set.of("openid"),
                false,
                false,
                Duration.ofMinutes(5),
                Duration.ofMinutes(5),
                Duration.ofHours(1));
    }
}
