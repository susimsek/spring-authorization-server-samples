package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminRoleServiceTest {

    @Mock private AuthorityRepository authorityRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdminAuditEventService adminAuditEventService;

    @Test
    void rolesReturnsMappedViews() {
        when(authorityRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(authority(2L, "ROLE_ADMIN"), authority(3L, "ROLE_AUDITOR")));

        assertThat(service().roles())
                .containsExactly(
                        new AdminRoleService.RoleView("ROLE_ADMIN"),
                        new AdminRoleService.RoleView("ROLE_AUDITOR"));
    }

    @Test
    void rejectsAnInvalidRoleName() {
        assertThatThrownBy(() -> service().createRole("role_user"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Role names must use ROLE_ uppercase format");
    }

    @Test
    void rejectsDuplicateRoleNames() {
        when(authorityRepository.existsByName("ROLE_AUDITOR")).thenReturn(true);

        assertThatThrownBy(() -> service().createRole("ROLE_AUDITOR"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Role is already registered");
    }

    @Test
    void createsRoleAndRecordsAuditEvent() {
        when(authorityRepository.existsByName("ROLE_AUDITOR")).thenReturn(false);
        when(authorityRepository.save(any(AuthorityEntity.class)))
                .thenAnswer(
                        invocation -> {
                            AuthorityEntity role = invocation.getArgument(0);
                            role.setId(9L);
                            return role;
                        });

        AdminRoleService.RoleView created = service().createRole("ROLE_AUDITOR");

        assertThat(created).isEqualTo(new AdminRoleService.RoleView("ROLE_AUDITOR"));
        verify(authorityRepository).save(any(AuthorityEntity.class));
        verify(adminAuditEventService).record("role.created", "role", "ROLE_AUDITOR");
    }

    @Test
    void deleteRoleRejectsMissingRole() {
        when(authorityRepository.findByName("ROLE_AUDITOR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().deleteRole("ROLE_AUDITOR"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Role not found");
    }

    @Test
    void deleteRoleRejectsProtectedAdministratorRole() {
        when(authorityRepository.findByName(AuthoritiesConstants.ADMIN))
                .thenReturn(Optional.of(authority(1L, AuthoritiesConstants.ADMIN)));

        assertThatThrownBy(() -> service().deleteRole(AuthoritiesConstants.ADMIN))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Role cannot be removed");

        verify(authorityRepository, never()).delete(any(AuthorityEntity.class));
    }

    @Test
    void deleteRoleRejectsProtectedUserRole() {
        when(authorityRepository.findByName(AuthoritiesConstants.USER))
                .thenReturn(Optional.of(authority(2L, AuthoritiesConstants.USER)));

        assertThatThrownBy(() -> service().deleteRole(AuthoritiesConstants.USER))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Role cannot be removed");

        verify(authorityRepository, never()).delete(any(AuthorityEntity.class));
    }

    @Test
    void deleteRoleRejectsAssignedCustomRole() {
        AuthorityEntity role = authority(4L, "ROLE_AUDITOR");
        when(authorityRepository.findByName("ROLE_AUDITOR")).thenReturn(Optional.of(role));
        when(userRepository.countByAuthoritiesId(4L)).thenReturn(2L);

        assertThatThrownBy(() -> service().deleteRole("ROLE_AUDITOR"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Role is assigned to one or more users");

        verify(authorityRepository, never()).delete(any(AuthorityEntity.class));
    }

    @Test
    void deletesAnUnassignedCustomRole() {
        AuthorityEntity role = authority(4L, "ROLE_AUDITOR");
        when(authorityRepository.findByName("ROLE_AUDITOR")).thenReturn(Optional.of(role));
        when(userRepository.countByAuthoritiesId(4L)).thenReturn(0L);

        service().deleteRole("ROLE_AUDITOR");

        verify(authorityRepository).delete(role);
        verify(adminAuditEventService).record("role.deleted", "role", "ROLE_AUDITOR");
    }

    private AdminRoleService service() {
        return new AdminRoleService(authorityRepository, userRepository, adminAuditEventService);
    }

    private static AuthorityEntity authority(Long id, String name) {
        AuthorityEntity authority = new AuthorityEntity();
        authority.setId(id);
        authority.setName(name);
        return authority;
    }
}
