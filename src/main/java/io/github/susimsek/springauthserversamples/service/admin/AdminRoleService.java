package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final AuthorityRepository authorityRepository;
    private final UserRepository userRepository;
    private final AdminAuditEventService adminAuditEventService;
    private final AdminUserService adminUserService;

    @Transactional(readOnly = true)
    public Page<RoleView> roles(String query, Pageable pageable) {
        return authorityRepository
                .findByNameContainingIgnoreCase(AdminSearch.normalize(query), pageable)
                .map(AdminRoleService::roleView);
    }

    @Transactional(readOnly = true)
    public Page<UserEntityView> availableUsers(String name, String query, Pageable pageable) {
        if (!authorityRepository.existsByName(name)) {
            throw ApiException.notFound("Role not found");
        }
        return userRepository
                .findAvailableRoleUsers(name, AdminSearch.normalize(query), pageable)
                .map(
                        user ->
                                new UserEntityView(
                                        user.getId(), user.getUsername(), user.isEnabled()));
    }

    @Transactional(readOnly = true)
    public RoleDetailView role(String name, String query, Pageable pageable) {
        AuthorityEntity role =
                authorityRepository
                        .findByName(name)
                        .orElseThrow(() -> ApiException.notFound("Role not found"));
        Page<UserEntityView> users =
                userRepository
                        .findByAuthoritiesNameAndUsernameContainingIgnoreCase(
                                name, AdminSearch.normalize(query), pageable)
                        .map(
                                user ->
                                        new UserEntityView(
                                                user.getId(),
                                                user.getUsername(),
                                                user.isEnabled()));
        return new RoleDetailView(
                role.getName(),
                userRepository.countByAuthoritiesId(role.getId()),
                AuthoritiesConstants.ADMIN.equals(name) || AuthoritiesConstants.USER.equals(name),
                users);
    }

    @Transactional(readOnly = true)
    public RoleDetailView role(String name, Pageable pageable) {
        return role(name, "", pageable);
    }

    @Transactional
    public RoleDetailView assignUser(
            String name, Long userId, String currentUsername, Pageable pageable) {
        adminUserService.assignRole(userId, name, currentUsername);
        adminAuditEventService.record("role.user.assigned", "role", name);
        return role(name, pageable);
    }

    @Transactional
    public RoleDetailView removeUser(
            String name, Long userId, String currentUsername, Pageable pageable) {
        adminUserService.removeRole(userId, name, currentUsername);
        adminAuditEventService.record("role.user.removed", "role", name);
        return role(name, pageable);
    }

    @Transactional
    public RoleView createRole(String name) {
        validateRoleName(name);
        if (authorityRepository.existsByName(name)) {
            throw ApiException.conflict(
                    "name", "admin_role_duplicate_name", "Role is already registered");
        }
        AuthorityEntity role = new AuthorityEntity();
        role.setName(name);
        RoleView view = roleView(authorityRepository.save(role));
        adminAuditEventService.record("role.created", "role", name);
        return view;
    }

    @Transactional
    public void deleteRole(String name) {
        AuthorityEntity role =
                authorityRepository
                        .findByName(name)
                        .orElseThrow(() -> ApiException.notFound("Role not found"));
        if (AuthoritiesConstants.ADMIN.equals(name) || AuthoritiesConstants.USER.equals(name)) {
            throw ApiException.badRequest("admin_role_protected", "Role cannot be removed");
        }
        if (userRepository.countByAuthoritiesId(role.getId()) > 0) {
            throw ApiException.badRequest(
                    "admin_role_assigned", "Role is assigned to one or more users");
        }
        authorityRepository.delete(role);
        adminAuditEventService.record("role.deleted", "role", name);
    }

    private static RoleView roleView(AuthorityEntity role) {
        return new RoleView(role.getName());
    }

    private static void validateRoleName(String name) {
        if (name == null || !name.matches("ROLE_[A-Z0-9_]+")) {
            throw ApiException.badRequest(
                    "name",
                    "admin_role_invalid_name",
                    "Role names must use ROLE_ uppercase format");
        }
    }

    public record RoleView(String name) {}

    public record UserEntityView(Long id, String username, boolean enabled) {}

    public record RoleDetailView(
            String name, long userCount, boolean protectedRole, Page<UserEntityView> users) {}
}
