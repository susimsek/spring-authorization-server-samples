package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.ClientScopeEntity;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.mapper.RegisteredClientMapper;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.ClientScopeRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminClientScopeServiceTest {

    private final ClientScopeRepository clientScopeRepository = mock(ClientScopeRepository.class);
    private final ClientRepository clientRepository = mock(ClientRepository.class);
    private final RegisteredClientMapper registeredClientMapper =
            mock(RegisteredClientMapper.class);
    private final AuthorizationServerMapperSupport mapperSupport =
            mock(AuthorizationServerMapperSupport.class);
    private final AdminAuditEventService adminAuditEventService =
            mock(AdminAuditEventService.class);

    @Test
    void findsClientScopeById() {
        ClientScopeEntity entity = scope("scope-1", "account-api");
        when(clientScopeRepository.findById("scope-1")).thenReturn(Optional.of(entity));

        var result = service().findOne("scope-1");

        assertThat(result.id()).isEqualTo("scope-1");
        assertThat(result.name()).isEqualTo("account-api");
    }

    @Test
    void rejectsUnknownClientScopeId() {
        when(clientScopeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findOne("missing"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Client scope not found");
    }

    private AdminClientScopeService service() {
        return new AdminClientScopeService(
                clientScopeRepository,
                clientRepository,
                registeredClientMapper,
                mapperSupport,
                adminAuditEventService);
    }

    private static ClientScopeEntity scope(String id, String name) {
        ClientScopeEntity entity = new ClientScopeEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setDisplayName("Account API");
        entity.setDescription("Account access");
        Instant timestamp = Instant.parse("2026-09-05T00:00:00Z");
        entity.setCreatedAt(timestamp);
        entity.setUpdatedAt(timestamp);
        return entity;
    }
}
