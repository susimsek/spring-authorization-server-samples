package io.github.susimsek.springauthserversamples.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OidcSessionStatusController {

    @GetMapping("/oidc/session-status")
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
