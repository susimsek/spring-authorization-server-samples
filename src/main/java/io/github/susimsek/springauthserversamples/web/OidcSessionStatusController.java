package io.github.susimsek.springauthserversamples.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "OAuth2 / OIDC", description = "Authorization server browser session endpoints.")
public class OidcSessionStatusController {

    @GetMapping("/oidc/session-status")
    @Operation(summary = "Get browser SSO session status")
    ResponseEntity<Map<String, Object>> sessionStatus(
            HttpServletRequest request, Principal principal) {
        HttpSession session = request.getSession(false);
        boolean authenticated = principal != null && session != null;
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        Map.of(
                                "authenticated",
                                authenticated,
                                "sessionId",
                                authenticated ? session.getId() : ""));
    }
}
