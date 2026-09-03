package io.github.susimsek.springauthserversamples.config.security;

import java.util.Set;

final class ConsoleClients {

    static final String ADMIN = "admin-console";
    static final String ACCOUNT = "account-console";
    static final Set<String> ALL = Set.of(ADMIN, ACCOUNT);

    private ConsoleClients() {}
}
