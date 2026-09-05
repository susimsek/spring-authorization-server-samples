package io.github.susimsek.springauthserversamples.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Constants for Spring Security authorities. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String USER_VIEWER = "ROLE_USER_VIEWER";

    public static final String USER_MANAGER = "ROLE_USER_MANAGER";

    public static final String CLIENT_VIEWER = "ROLE_CLIENT_VIEWER";

    public static final String CLIENT_MANAGER = "ROLE_CLIENT_MANAGER";

    public static final String PREVIOUS_ADMINISTRATOR = "ROLE_PREVIOUS_ADMINISTRATOR";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";
}
