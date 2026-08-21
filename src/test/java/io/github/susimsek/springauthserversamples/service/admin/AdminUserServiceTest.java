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
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.UserAvatarRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserAvatarRepository userAvatarRepository;
    @Mock private AuthorityRepository authorityRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private AuthorizationRepository authorizationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminAuditEventService adminAuditEventService;

    @Test
    void userReturnsMappedViewWithAvatar() {
        UserEntity target = user(7L, "alice", AuthoritiesConstants.USER);
        UserEntity administrator = user(8L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));
        when(userAvatarRepository.findVersionByUserId(7L))
                .thenReturn(Optional.of(avatar(7L, "avatar-7", 42L)));

        AdminUserService.UserView view = service().user(7L, "administrator");

        assertThat(view.id()).isEqualTo(7L);
        assertThat(view.username()).isEqualTo("alice");
        assertThat(view.avatarUrl()).isEqualTo("/avatars/avatar-7?v=42");
        assertThat(view.authorities()).containsExactly(AuthoritiesConstants.USER);
    }

    @Test
    void userRejectsManagingAdministratorForNonAdminManager() {
        UserEntity target = user(5L, "administrator", AuthoritiesConstants.ADMIN);
        UserEntity manager = user(6L, "manager", "ROLE_USER_MANAGER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service().user(5L, "manager"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Only an administrator can manage an administrator");
    }

    @Test
    void createUserRejectsBlankUsername() {
        assertThatThrownBy(() -> service().createUser(" ", "password-123", true, Set.of()))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Username is required");
    }

    @Test
    void createUserRejectsShortPassword() {
        assertThatThrownBy(() -> service().createUser("alice", "short", true, Set.of()))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Password must be at least 8 characters");
    }

    @Test
    void createsUserWithEncodedPasswordAndDefaultRole() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(authorityRepository.findByNameIn(Set.of(AuthoritiesConstants.USER)))
                .thenReturn(List.of(authority(1L, AuthoritiesConstants.USER)));
        when(passwordEncoder.encode("password-123")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(
                        invocation -> {
                            UserEntity saved = invocation.getArgument(0);
                            saved.setId(99L);
                            return saved;
                        });

        AdminUserService.UserView created =
                service().createUser("alice", "password-123", true, Set.of());

        assertThat(created.username()).isEqualTo("alice");
        assertThat(created.enabled()).isTrue();
        assertThat(created.avatarUrl()).isNull();
        assertThat(created.authorities()).containsExactly(AuthoritiesConstants.USER);
        verify(adminAuditEventService).record("user.created", "user", created.id().toString());
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(user(1L, "alice", AuthoritiesConstants.USER)));

        assertThatThrownBy(() -> service().createUser("alice", "password-123", true, Set.of()))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Username is already registered");
    }

    @Test
    void createUserRejectsUnknownRole() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(authorityRepository.findByNameIn(Set.of("ROLE_UNKNOWN"))).thenReturn(List.of());

        assertThatThrownBy(
                        () ->
                                service()
                                        .createUser(
                                                "alice",
                                                "password-123",
                                                true,
                                                Set.of("ROLE_UNKNOWN")))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("One or more roles are invalid");
    }

    @Test
    void updateUserRejectsBlankUsername() {
        assertThatThrownBy(
                        () ->
                                service()
                                        .updateUser(
                                                5L,
                                                " ",
                                                true,
                                                Set.of(AuthoritiesConstants.USER),
                                                "administrator"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Username is required");
    }

    @Test
    void updateUserRejectsDuplicateUsernameWhenChangingName() {
        UserEntity target = user(5L, "alice", AuthoritiesConstants.USER);
        UserEntity administrator = user(6L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));
        when(userRepository.findByUsername("bob"))
                .thenReturn(Optional.of(user(9L, "bob", AuthoritiesConstants.USER)));

        assertThatThrownBy(
                        () ->
                                service()
                                        .updateUser(
                                                5L,
                                                "bob",
                                                true,
                                                Set.of(AuthoritiesConstants.USER),
                                                "administrator"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Username is already registered");
    }

    @Test
    void updateUserRejectsDisablingOwnAccount() {
        UserEntity administrator = user(5L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(administrator));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        assertThatThrownBy(
                        () ->
                                service()
                                        .updateUser(
                                                5L,
                                                "administrator",
                                                false,
                                                Set.of(AuthoritiesConstants.ADMIN),
                                                "administrator"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("You cannot disable your own account");
    }

    @Test
    void updateRejectsRoleEscalationForNonAdminManager() {
        UserEntity target = user(5L, "operator", AuthoritiesConstants.USER);
        UserEntity manager = user(6L, "manager", "ROLE_USER_MANAGER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        assertThatThrownBy(
                        () ->
                                service()
                                        .updateUser(
                                                5L,
                                                "operator",
                                                true,
                                                Set.of(AuthoritiesConstants.ADMIN),
                                                "manager"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("You can only assign roles you already have");
    }

    @Test
    void lastAdministratorCannotRemoveItsAdministratorRole() {
        UserEntity administrator = user(5L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(administrator));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));
        when(userRepository.countByAuthoritiesName(AuthoritiesConstants.ADMIN)).thenReturn(1L);

        assertThatThrownBy(
                        () ->
                                service()
                                        .updateUser(
                                                5L,
                                                "administrator",
                                                true,
                                                Set.of(AuthoritiesConstants.USER),
                                                "administrator"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("The last administrator must be retained");
    }

    @Test
    void updateUserReplacesFieldsInvalidatesSessionsAndAudits() {
        UserEntity target = user(5L, "alice", AuthoritiesConstants.USER);
        UserEntity administrator = user(6L, "administrator", AuthoritiesConstants.ADMIN);
        AuthorityEntity adminRole = authority(2L, AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));
        when(authorityRepository.findByNameIn(Set.of(AuthoritiesConstants.ADMIN)))
                .thenReturn(List.of(adminRole));
        when(userAvatarRepository.findVersionByUserId(5L)).thenReturn(Optional.empty());

        AdminUserService.UserView updated =
                service()
                        .updateUser(
                                5L,
                                "alice-updated",
                                false,
                                Set.of(AuthoritiesConstants.ADMIN),
                                "administrator");

        assertThat(updated.username()).isEqualTo("alice-updated");
        assertThat(updated.enabled()).isFalse();
        assertThat(updated.authorities()).containsExactly(AuthoritiesConstants.ADMIN);
        verify(userSessionRepository).deleteByPrincipalName("alice");
        verify(authorizationRepository).deleteByPrincipalName("alice");
        verify(adminAuditEventService).record("user.updated", "user", "5");
    }

    @Test
    void userManagerCannotResetAnAdministratorsPassword() {
        UserEntity administrator = user(5L, "administrator", AuthoritiesConstants.ADMIN);
        UserEntity manager = user(6L, "manager", "ROLE_USER_MANAGER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(administrator));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service().changePassword(5L, "new-password", "manager"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Only an administrator can manage an administrator");
    }

    @Test
    void changePasswordRejectsShortPasswords() {
        assertThatThrownBy(() -> service().changePassword(5L, "short", "manager"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Password must be at least 8 characters");
    }

    @Test
    void changePasswordEncodesPasswordInvalidatesSessionsAndAudits() {
        UserEntity user = user(5L, "alice", AuthoritiesConstants.USER);
        UserEntity administrator = user(6L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

        service().changePassword(5L, "new-password", "administrator");

        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(userSessionRepository).deleteByPrincipalName("alice");
        verify(authorizationRepository).deleteByPrincipalName("alice");
        verify(adminAuditEventService).record("user.password.updated", "user", "5");
    }

    @Test
    void userManagerCannotDisableAnAdministrator() {
        UserEntity administrator = user(5L, "administrator", AuthoritiesConstants.ADMIN);
        UserEntity manager = user(6L, "manager", "ROLE_USER_MANAGER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(administrator));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service().setUserEnabled(5L, false, "manager"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Only an administrator can manage an administrator");
    }

    @Test
    void disablingOwnAccountIsRejected() {
        UserEntity administrator = user(5L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(administrator));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        assertThatThrownBy(() -> service().setUserEnabled(5L, false, "administrator"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("You cannot disable your own account");
    }

    @Test
    void disablingLastAdministratorIsRejected() {
        UserEntity administrator = user(5L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(administrator));
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user(7L, "admin", AuthoritiesConstants.ADMIN)));
        when(userRepository.countByAuthoritiesName(AuthoritiesConstants.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service().setUserEnabled(5L, false, "admin"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("The last administrator must be retained");
    }

    @Test
    void disablingUserInvalidatesSessionsAndAudits() {
        UserEntity target = user(5L, "alice", AuthoritiesConstants.USER);
        UserEntity administrator = user(6L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        service().setUserEnabled(5L, false, "administrator");

        assertThat(target.isEnabled()).isFalse();
        verify(userSessionRepository).deleteByPrincipalName("alice");
        verify(authorizationRepository).deleteByPrincipalName("alice");
        verify(adminAuditEventService).record("user.enabled.updated", "user", "5");
    }

    @Test
    void enablingUserDoesNotInvalidateSessions() {
        UserEntity target = user(5L, "alice", AuthoritiesConstants.USER);
        target.setEnabled(false);
        UserEntity administrator = user(6L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        service().setUserEnabled(5L, true, "administrator");

        assertThat(target.isEnabled()).isTrue();
        verify(userSessionRepository, never()).deleteByPrincipalName("alice");
        verify(authorizationRepository, never()).deleteByPrincipalName("alice");
        verify(adminAuditEventService).record("user.enabled.updated", "user", "5");
    }

    @Test
    void deleteRejectsCurrentUserDeletion() {
        UserEntity administrator = user(5L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(administrator));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        assertThatThrownBy(() -> service().deleteUser(5L, "administrator"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("You cannot delete your own account");
    }

    @Test
    void deleteRejectsRemovingLastAdministrator() {
        UserEntity administrator = user(5L, "administrator", AuthoritiesConstants.ADMIN);
        UserEntity otherAdministrator = user(6L, "other-admin", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(administrator));
        when(userRepository.findByUsername("other-admin"))
                .thenReturn(Optional.of(otherAdministrator));
        when(userRepository.countByAuthoritiesName(AuthoritiesConstants.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service().deleteUser(5L, "other-admin"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("The last administrator must be retained");
    }

    @Test
    void deleteUserInvalidatesSessionsDeletesEntityAndAudits() {
        UserEntity target = user(5L, "alice", AuthoritiesConstants.USER);
        UserEntity administrator = user(6L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        service().deleteUser(5L, "administrator");

        verify(userSessionRepository).deleteByPrincipalName("alice");
        verify(authorizationRepository).deleteByPrincipalName("alice");
        verify(userRepository).delete(target);
        verify(adminAuditEventService).record("user.deleted", "user", "5");
    }

    @Test
    void usersFiltersByEnabledFlagAndMapsAvatar() {
        UserEntity user = user(7L, "alice", AuthoritiesConstants.USER);
        when(userRepository.findByUsernameContainingIgnoreCaseAndEnabled(
                        "alice", true, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userAvatarRepository.findVersionsByUserIdIn(List.of(7L)))
                .thenReturn(List.of(avatar(7L, "avatar-1", 42L)));

        AdminUserService.UserView view =
                service().users("alice", true, Pageable.unpaged()).getContent().getFirst();

        assertThat(view.avatarUrl()).isEqualTo("/avatars/avatar-1?v=42");
        assertThat(view.authorities()).containsExactly(AuthoritiesConstants.USER);
    }

    @Test
    void usersUsesUnfilteredQueryWhenEnabledFlagIsNull() {
        UserEntity user = user(7L, "alice", AuthoritiesConstants.USER);
        when(userRepository.findByUsernameContainingIgnoreCase("alice", Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userAvatarRepository.findVersionsByUserIdIn(List.of(7L))).thenReturn(List.of());

        AdminUserService.UserView view =
                service().users(" alice ", null, Pageable.unpaged()).getContent().getFirst();

        assertThat(view.avatarUrl()).isNull();
        assertThat(view.username()).isEqualTo("alice");
    }

    @Test
    void usersReturnsEmptyPageWithoutLoadingAvatars() {
        when(userRepository.findByUsernameContainingIgnoreCase("", Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service().users(null, null, Pageable.unpaged()).getContent()).isEmpty();
        verify(userAvatarRepository, never()).findVersionsByUserIdIn(any());
    }

    @Test
    void assertCanManageUsernameThrowsNotFoundForMissingUser() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().assertCanManageUsername("missing", "admin"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("User not found");
    }

    @Test
    void assertCanManageUsernameAllowsManageableUser() {
        UserEntity target = user(5L, "alice", AuthoritiesConstants.USER);
        UserEntity administrator = user(6L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        service().assertCanManageUsername("alice", "administrator");
    }

    @Test
    void requireManageableUserReturnsUserForAdministrator() {
        UserEntity target = user(5L, "operator", AuthoritiesConstants.USER);
        UserEntity administrator = user(6L, "administrator", AuthoritiesConstants.ADMIN);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        assertThat(service().requireManageableUser(5L, "administrator")).isSameAs(target);
    }

    private AdminUserService service() {
        return new AdminUserService(
                userRepository,
                userAvatarRepository,
                authorityRepository,
                userSessionRepository,
                authorizationRepository,
                passwordEncoder,
                adminAuditEventService);
    }

    private static AuthorityEntity authority(Long id, String role) {
        AuthorityEntity authority = new AuthorityEntity();
        authority.setId(id);
        authority.setName(role);
        return authority;
    }

    private static UserEntity user(Long id, String username, String role) {
        return user(id, username, Set.of(role));
    }

    private static UserEntity user(Long id, String username, Set<String> roles) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEnabled(true);
        user.setAuthorities(
                roles.stream().map(role -> authority(null, role)).collect(Collectors.toSet()));
        return user;
    }

    private static UserAvatarRepository.AvatarVersion avatar(
            Long userId, String publicId, long epochMilli) {
        return new UserAvatarRepository.AvatarVersion() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public String getPublicId() {
                return publicId;
            }

            @Override
            public Instant getUpdatedAt() {
                return Instant.ofEpochMilli(epochMilli);
            }
        };
    }
}
