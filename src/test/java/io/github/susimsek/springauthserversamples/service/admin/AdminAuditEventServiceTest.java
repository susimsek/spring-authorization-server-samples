package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.github.susimsek.springauthserversamples.domain.AdminEventEntity;
import io.github.susimsek.springauthserversamples.repository.AdminEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminAuditEventServiceTest {

    private final AdminEventRepository eventRepository =
            org.mockito.Mockito.mock(AdminEventRepository.class);
    private final AdminAuditEventService service = new AdminAuditEventService(eventRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsTheAuthenticatedActor() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "admin", "n/a", java.util.List.of()));

        service.record("client.created", "client", "client-id");

        ArgumentCaptor<AdminEventEntity> event = ArgumentCaptor.forClass(AdminEventEntity.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getActor()).isEqualTo("admin");
        assertThat(event.getValue().getAction()).isEqualTo("client.created");
    }

    @Test
    void usesSystemForAnonymousEvents() {
        service.avatarDeleted(7L);

        ArgumentCaptor<AdminEventEntity> event = ArgumentCaptor.forClass(AdminEventEntity.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getActor()).isEqualTo("system");
        assertThat(event.getValue().getTargetId()).isEqualTo("7");
    }
}
