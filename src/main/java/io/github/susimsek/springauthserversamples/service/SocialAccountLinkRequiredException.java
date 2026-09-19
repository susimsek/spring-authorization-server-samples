package io.github.susimsek.springauthserversamples.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/** Signals that a verified social identity must be linked after local account re-authentication. */
public final class SocialAccountLinkRequiredException extends OAuth2AuthenticationException {

    public static final String ERROR_CODE = "account_link_required";

    private final Map<String, String> pendingLink;

    public SocialAccountLinkRequiredException(String provider, String subject, String email) {
        super(
                new OAuth2Error(ERROR_CODE),
                "A local account with this email already exists; authenticate locally to link"
                        + " the social account");
        Map<String, String> link = new LinkedHashMap<>();
        link.put("provider", provider);
        link.put("subject", subject);
        link.put("email", email == null ? "" : email);
        this.pendingLink = link;
    }

    public Map<String, String> pendingLink() {
        return pendingLink;
    }
}
