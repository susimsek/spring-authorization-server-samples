package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.dto.admin.AdminWhoAmIDTO;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiController
@RequestMapping("/api/admin")
@Tag(name = "Admin - Identity", description = "Current administrator identity and access flags.")
public class AdminWhoAmIController {

    @GetMapping("/whoami")
    @Operation(
            summary = "Get current administrator",
            description =
                    "Returns the authenticated administrator and access flags calculated from"
                            + " assigned authorities.")
    @ApiResponse(responseCode = "200", description = "Current administrator identity returned.")
    AdminWhoAmIDTO whoAmI(Authentication authentication) {
        Set<String> authorities =
                authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(Collectors.toUnmodifiableSet());

        return new AdminWhoAmIDTO(
                authentication.getName(),
                authorities.stream().sorted().toList(),
                Map.ofEntries(
                        Map.entry("isAdmin", hasAny(authorities, "ROLE_ADMIN")),
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
}
