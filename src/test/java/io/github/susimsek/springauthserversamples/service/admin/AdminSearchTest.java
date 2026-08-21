package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminSearchTest {

    @Test
    void normalizesBlankSearchTerms() {
        assertThat(AdminSearch.normalize(null)).isEmpty();
        assertThat(AdminSearch.normalize("  client  ")).isEqualTo("client");
    }

    @Test
    void rejectsSearchTermsOverTheLimit() {
        assertThatThrownBy(() -> AdminSearch.normalize("a".repeat(101)))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Search query must not exceed 100 characters");
    }
}
