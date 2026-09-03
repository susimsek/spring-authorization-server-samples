package io.github.susimsek.springauthserversamples.service.account;

import io.github.susimsek.springauthserversamples.domain.UserAction;
import java.util.Locale;

public record UserActionEmailEvent(
        UserAction action, String recipient, String username, Locale locale, String actionUrl) {}
