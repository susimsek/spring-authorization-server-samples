package io.github.susimsek.springauthserversamples.web.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AdminApi
@RequestMapping("/api/admin")
public class AdminWhoAmIController {

    @GetMapping("/whoami")
    AdminWhoAmIView whoAmI(Authentication authentication) {
        Set<String> authorities =
                authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(Collectors.toUnmodifiableSet());

        return new AdminWhoAmIView(
                authentication.getName(),
                authorities.stream().sorted().toList(),
                Map.ofEntries(
                        Map.entry(
                                "viewClients",
                                hasAny(
                                        authorities,
                                        "ROLE_ADMIN",
                                        "ROLE_CLIENT_VIEWER",
                                        "ROLE_CLIENT_MANAGER")),
                        Map.entry(
                                "manageClients",
                                hasAny(authorities, "ROLE_ADMIN", "ROLE_CLIENT_MANAGER")),
                        Map.entry(
                                "viewUsers",
                                hasAny(
                                        authorities,
                                        "ROLE_ADMIN",
                                        "ROLE_USER_VIEWER",
                                        "ROLE_USER_MANAGER")),
                        Map.entry(
                                "manageUsers",
                                hasAny(authorities, "ROLE_ADMIN", "ROLE_USER_MANAGER")),
                        Map.entry(
                                "viewRoles",
                                hasAny(authorities, "ROLE_ADMIN", "ROLE_USER_MANAGER")),
                        Map.entry("manageRoles", hasAny(authorities, "ROLE_ADMIN")),
                        Map.entry(
                                "viewSessions",
                                hasAny(authorities, "ROLE_ADMIN", "ROLE_SESSION_MANAGER")),
                        Map.entry(
                                "manageSessions",
                                hasAny(authorities, "ROLE_ADMIN", "ROLE_SESSION_MANAGER")),
                        Map.entry(
                                "viewConsents",
                                hasAny(
                                        authorities,
                                        "ROLE_ADMIN",
                                        "ROLE_USER_VIEWER",
                                        "ROLE_USER_MANAGER")),
                        Map.entry(
                                "manageConsents",
                                hasAny(authorities, "ROLE_ADMIN", "ROLE_USER_MANAGER")),
                        Map.entry("viewKeys", hasAny(authorities, "ROLE_ADMIN")),
                        Map.entry("manageKeys", hasAny(authorities, "ROLE_ADMIN"))));
    }

    private static boolean hasAny(Set<String> authorities, String... candidates) {
        for (String candidate : candidates) {
            if (authorities.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    record AdminWhoAmIView(
            String username, List<String> authorities, Map<String, Boolean> access) {}
}
