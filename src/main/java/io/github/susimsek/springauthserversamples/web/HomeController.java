package io.github.susimsek.springauthserversamples.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    Map<String, Object> index() {
        return Map.of(
                "application", "spring-authorization-server-samples",
                "metadata", "/.well-known/openid-configuration",
                "jwkSet", "/oauth2/jwks",
                "tokenEndpoint", "/oauth2/token",
                "authorizationEndpoint", "/oauth2/authorize");
    }
}
