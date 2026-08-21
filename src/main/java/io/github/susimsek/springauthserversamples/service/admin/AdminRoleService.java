package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final AuthorityRepository authorityRepository;
    private final UserRepository userRepository;
    private final AdminAuditEventService adminAuditEventService;

    @Transactional(readOnly = true)
    public List<RoleView> roles() {
        return authorityRepository.findAllByOrderByNameAsc().stream()
                .map(AdminRoleService::roleView)
                .toList();
    }

    @Transactional
    public RoleView createRole(String name) {
        validateRoleName(name);
        if (authorityRepository.existsByName(name)) {
            throw AdminClientException.conflict(
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
                        .orElseThrow(() -> AdminClientException.notFound("Role not found"));
        if (AuthoritiesConstants.ADMIN.equals(name) || AuthoritiesConstants.USER.equals(name)) {
            throw AdminClientException.badRequest("admin_role_protected", "Role cannot be removed");
        }
        if (userRepository.countByAuthoritiesId(role.getId()) > 0) {
            throw AdminClientException.badRequest(
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
            throw AdminClientException.badRequest(
                    "name",
                    "admin_role_invalid_name",
                    "Role names must use ROLE_ uppercase format");
        }
    }

    public record RoleView(String name) {}
}
