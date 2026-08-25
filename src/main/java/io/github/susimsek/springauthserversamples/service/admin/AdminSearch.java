package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.service.error.ApiException;

final class AdminSearch {

    private static final int MAX_QUERY_LENGTH = 100;

    private AdminSearch() {}

    static String normalize(String query) {
        String normalized = query == null ? "" : query.strip();
        if (normalized.length() > MAX_QUERY_LENGTH) {
            throw ApiException.badRequest(
                    "admin_search_too_long", "Search query must not exceed 100 characters");
        }
        return normalized;
    }
}
