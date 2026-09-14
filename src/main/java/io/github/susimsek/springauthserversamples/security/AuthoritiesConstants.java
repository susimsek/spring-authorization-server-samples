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

    public static final String GROUP_VIEWER = "ROLE_GROUP_VIEWER";

    public static final String GROUP_MANAGER = "ROLE_GROUP_MANAGER";

    public static final String CLIENT_VIEWER = "ROLE_CLIENT_VIEWER";

    public static final String CLIENT_MANAGER = "ROLE_CLIENT_MANAGER";

    public static final String EVENT_VIEWER = "ROLE_EVENT_VIEWER";

    public static final String EVENT_MANAGER = "ROLE_EVENT_MANAGER";

    public static final String PREVIOUS_ADMINISTRATOR = "ROLE_PREVIOUS_ADMINISTRATOR";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";
}
