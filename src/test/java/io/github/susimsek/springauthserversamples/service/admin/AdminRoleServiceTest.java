package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminRoleServiceTest {

    @Mock private AuthorityRepository authorityRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdminAuditEventService adminAuditEventService;
    @Mock private AdminUserService adminUserService;

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

    @Test
    void searchesAssignedUsersOnTheServer() {
        AuthorityEntity role = authority(4L, "ROLE_AUDITOR");
        UserEntity alice = new UserEntity();
        alice.setId(10L);
        alice.setUsername("alice");
        alice.setEnabled(true);
        var pageable = PageRequest.of(0, 20);
        when(authorityRepository.findByName("ROLE_AUDITOR")).thenReturn(Optional.of(role));
        when(userRepository.findByAuthoritiesNameAndUsernameContainingIgnoreCase(
                        "ROLE_AUDITOR", "ali", pageable))
                .thenReturn(new PageImpl<>(List.of(alice), pageable, 1));
        when(userRepository.countByAuthoritiesId(4L)).thenReturn(1L);

        var result = service().role("ROLE_AUDITOR", "  ali  ", pageable);

        assertThat(result.users().getContent())
                .containsExactly(new AdminRoleService.UserEntityView(10L, "alice", true));
        assertThat(result.userCount()).isEqualTo(1L);
    }

    @Test
    void searchesOnlyUsersNotAlreadyAssignedToRole() {
        UserEntity bob = new UserEntity();
        bob.setId(11L);
        bob.setUsername("bob");
        bob.setEnabled(true);
        var pageable = PageRequest.of(0, 10);
        when(authorityRepository.existsByName("ROLE_AUDITOR")).thenReturn(true);
        when(userRepository.findAvailableRoleUsers("ROLE_AUDITOR", "bo", pageable))
                .thenReturn(new PageImpl<>(List.of(bob), pageable, 1));

        assertThat(service().availableUsers("ROLE_AUDITOR", " bo ", pageable).getContent())
                .containsExactly(new AdminRoleService.UserEntityView(11L, "bob", true));
    }

    private AdminRoleService service() {
        return new AdminRoleService(
                authorityRepository, userRepository, adminAuditEventService, adminUserService);
    }

    private static AuthorityEntity authority(Long id, String name) {
        AuthorityEntity authority = new AuthorityEntity();
        authority.setId(id);
        authority.setName(name);
        return authority;
    }
}
