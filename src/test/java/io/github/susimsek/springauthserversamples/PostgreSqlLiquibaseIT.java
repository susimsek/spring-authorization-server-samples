package io.github.susimsek.springauthserversamples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Verifies the production database dialect and the complete Liquibase bootstrap. The test is opt-in
 * with {@code -Dpostgres.it=true}; hosts without Docker skip it, while CI runners with Docker
 * exercise PostgreSQL directly. The default H2 test profile is never replaced.
 */
@EnabledIfSystemProperty(named = "postgres.it", matches = "true")
@SpringBootTest(classes = SpringAuthorizationServerSamplesApplication.class)
class PostgreSqlLiquibaseIT {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("authorization_server_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void startPostgres() {
        POSTGRES.start();
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void liquibaseCreatesTheSchemaAndLoadsOAuthClientSeedData() {
        Integer changesets =
                jdbcTemplate.queryForObject(
                        "select count(*) from databasechangelog", Integer.class);
        Integer registeredClients =
                jdbcTemplate.queryForObject(
                        "select count(*) from oauth2_registered_client where client_id = ?",
                        Integer.class,
                        "demo-client");

        assertThat(changesets).isPositive();
        assertThat(registeredClients).isEqualTo(1);
    }
}
