# AI Agent Guidelines

This repo is a Java 25 + Spring Boot 4.1 sample application for the Authorization Server support integrated into **Spring Security 7**. It uses Spring Security 7 Authorization Server, Spring Data JPA, H2, PostgreSQL, Liquibase XML changelogs, Lombok, Caffeine/JCache, Spotless, Checkstyle, Sonar, JaCoCo, Helm, Terraform, Docker Compose, and GraalVM Native Image support.

## Table of Contents

1.  [Agent MCP Usage Guidelines](#agent-mcp-usage-guidelines)
2.  [Quick Reference](#quick-reference)
3.  [Prerequisites](#prerequisites)
4.  [Project Structure](#project-structure)
5.  [Code Style and Quality Gates](#code-style-and-quality-gates)
6.  [Testing Guidelines](#testing-guidelines)
7.  [Native Image & AOT Guidance](#native-image--aot-guidance)
8.  [Authentication](#authentication)
9.  [Development Guidelines](#development-guidelines)
10. [Pull Request and Commit Guidelines](#pull-request-and-commit-guidelines)
11. [Review Process & What Reviewers Look For](#review-process--what-reviewers-look-for)
12. [Common Mistakes to Avoid](#common-mistakes-to-avoid)

## Agent MCP Usage Guidelines

- Use Context7 when library/API documentation is needed for Spring Boot, Spring Security 7 Authorization Server, Spring Data JPA, Hibernate, Liquibase, Maven plugins, Helm, Terraform, or related setup/configuration details.
- Prefer official documentation or primary sources for framework behavior.

## Quick Reference

| Action | Command |
| --- | --- |
| Run dev server | `./mvnw spring-boot:run` |
| Run prod server | `./mvnw -Pprod spring-boot:run` |
| Run prod + docker-compose | `./mvnw -Pprod,docker-compose spring-boot:run` |
| Unit tests | `./mvnw test` |
| Integration tests | `./mvnw failsafe:integration-test failsafe:verify` |
| Performance tests | `./mvnw gatling:test` |
| Full verify | `./mvnw verify` |
| Format check | `./mvnw spotless:check` |
| Format apply | `./mvnw spotless:apply` |
| Checkstyle | `./mvnw checkstyle:check` |
| Package | `./mvnw -DskipTests package` |
| Native executable | `./mvnw -Pprod,native -DskipTests native:compile` |
| Sonar scan | `./mvnw -Psonar sonar:sonar` |

## Prerequisites

- Java: `25`
- Maven: use the wrapper (`./mvnw`)
- Optional local infrastructure tooling:
  - `terraform`
  - `kind`
  - `kubectl`
  - Docker or Podman
- Optional OAuth2 testing tools:
  - `curl`
  - `jq`
- Optional native build tooling:
  - GraalVM Native Image

## Project Structure

- `frontend`: Next.js App Router + TypeScript login UI built with pnpm, React-Bootstrap, Bootstrap, and Font Awesome. Maven exports it into Spring Boot static resources.

- Application root: `src/main/java/io/github/susimsek/springauthserversamples`
  - `config`: Spring configuration
    - `aot`: GraalVM Native Image runtime hints (`NativeRuntimeHints`)
    - `cache`: Spring Cache and Hibernate second-level cache configuration
    - `security`: Authorization Server security chains, localized handlers, security utilities, and the database-backed JWK source
  - `domain`: JPA entities (`UserEntity`, `AuthorityEntity`) and auditing base class
  - `repository`: Spring Data JPA repositories
  - `security`: authority constants, user-details service, and security utilities
  - `web`: sample landing endpoint
- Application config: `src/main/resources/config/application.yml`
- Liquibase:
  - Master: `src/main/resources/db/changelog/db.changelog-master.xml`
  - Changelogs: `src/main/resources/db/changelog/changes`
  - CSV seed data: `src/main/resources/db/data`
- i18n messages:
  - Default English: `src/main/resources/i18n/messages.properties`
  - Turkish: `src/main/resources/i18n/messages_tr.properties`
- Native image metadata:
  - Runtime hints: `src/main/java/.../config/aot/NativeRuntimeHints.java`
  - Resource-based GraalVM config: `src/main/resources/META-INF/native-image/**`
- Docker compose: `src/main/docker/*.yml`
- Helm chart: `helm/spring-authorization-server-samples`
- Terraform local infrastructure: `terraform`
- Tests: `src/test/java`
  - Unit and integration tests: `src/test/java/io/github/susimsek/springauthserversamples`
  - Gatling performance tests: `src/test/java/gatling/simulations`

## Code Style and Quality Gates

- Formatting: Spotless with `google-java-format` AOSP.
- Checkstyle runs in the `validate` phase.
- Follow `.editorconfig`:
  - LF line endings
  - final newline
  - no trailing whitespace
  - Java indent size 4
  - YAML indent size 2
- Avoid global coverage excludes for handwritten code.
- Do not edit generated or build output under `target/`.
- When you change code: apply formatting and ensure tests pass (`./mvnw spotless:apply` and `./mvnw test`).

## Testing Guidelines

- Unit tests live under `src/test/java` and use singular class names ending with `Test`.
- Integration tests live under `src/test/java`, use singular class names ending with `IT`, and use the `@IntegrationTest` meta-annotation where applicable.
- Keep test class names class-based, for example:
  - `AuthorityEntityTest`
  - `UserEntityTest`
  - `SecurityConfigTest`
  - `DomainUserDetailsServiceTest`
  - `AuthorizationServerEndpointsIT`
- Keep integration tests focused on end-to-end wiring of Spring Boot, Spring Security, authorization server endpoints, persistence, and Liquibase-seeded client data.
- Maintain strong JaCoCo coverage for handwritten application code.
- The current verification command is:
  - `./mvnw verify`

### Integration Tests

- Run all integration tests:
  - `./mvnw failsafe:integration-test failsafe:verify`
- Run a single integration test:
  - `./mvnw -Dit.test=AuthorizationServerEndpointsIT failsafe:integration-test failsafe:verify`
- Integration tests currently verify:
  - OpenID Provider metadata
  - JWK Set exposure
  - client credentials token flow
  - token introspection flow
  - readiness probe exposure
- Prefer HTTP-level tests for Authorization Server behavior instead of trying to unit-test framework internals.
- JaCoCo reports:
  - unit: `target/site/jacoco/jacoco.xml`
  - integration: `target/site/jacoco-it/jacoco.xml`

### Performance Tests

- Performance tests are implemented with Gatling under `src/test/java/gatling/simulations`.
- Shared Gatling runtime defaults live in `src/test/java/gatling/GatlingDefaults.java`.
- Run all simulations:
  - `./mvnw gatling:test`
- Common runtime overrides:
  - `./mvnw gatling:test -DhttpHost=127.0.0.1 -DhttpPort=9090 -Dusers=5 -Dramp=1 -Dduration=1 -DclientId=demo-client -DclientSecret=demo-secret -Dscope=openid -Dlocale=tr`
- The checked-in `OAuth2Simulation` covers:
  - OpenID discovery
  - JWK Set lookup
  - client credentials token issuance
  - token introspection

### OAuth2 Testing

- Server port: `9090`.
- OIDC discovery:

```bash
curl http://localhost:9090/.well-known/openid-configuration
```

- JWK Set:

```bash
curl http://localhost:9090/oauth2/jwks
```

- Seeded client credentials token:

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: en' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d scope=openid \
  -d grant_type=client_credentials \
  http://localhost:9090/oauth2/token
```

- Token introspection:

```bash
TOKEN=$(curl -s -u demo-client:demo-secret \
  -H 'Accept-Language: en' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=client_credentials \
  -d scope=openid \
  http://localhost:9090/oauth2/token | jq -r '.access_token')

curl -u demo-client:demo-secret \
  -H 'Accept-Language: en' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d token="${TOKEN}" \
  http://localhost:9090/oauth2/introspect
```

- Health check:

```bash
curl http://localhost:9090/actuator/health/readiness
```

## Native Image & AOT Guidance

- Native builds use Spring Boot AOT and GraalVM Native Build Tools.
- Build with `./mvnw -Pprod,native -DskipTests native:compile`.
- Runtime hints live in `config/aot/NativeRuntimeHints`; resource-based reflection/resource config lives under `src/main/resources/META-INF/native-image`.
- Update `NativeRuntimeHints` when adding:
  - New Liquibase XML/CSV resources
  - New i18n message bundles
  - New framework resources that native image must keep
- If native runtime fails because resources are missing, add focused `RuntimeHints` instead of broad classpath inclusion.
- Pay attention to Liquibase XML/CSV resources, i18n bundles, H2, Hibernate, Hibernate JCache, and the custom JPA-backed Authorization Server services when changing native-sensitive code.

## Authentication

- Form login is used for end-user authentication.
- Registered OAuth2 clients are stored in `oauth2_registered_client`.
- Seeded users for local dev: `admin/admin` and `user/user`.
- Seeded OAuth2 clients for local dev: `demo-client/demo-secret` and `pkce-client/demo-secret`.
- The issuer is configured via `app.authorization-server.issuer`.
- Client secrets are stored as BCrypt hashes in Liquibase seed data.
- The sample enables OpenID Connect 1.0.
- Exactly one `oauth2_key` row must be active; inactive rows are published as public-only verification keys.

## Development Guidelines

### Architecture

- Keep application-specific behavior in repository/config/security layers; do not try to reimplement Spring Authorization Server internals unnecessarily.
- Keep the custom JPA-backed Authorization Server services aligned with Spring Authorization Server's core model and JDBC schema semantics.
- Do not add ad hoc runtime seeders when a value belongs in Liquibase-managed seed data.

### Validation

- This sample does not use the gRPC Protovalidate layer from the original project.
- Prefer request validation through Spring Security / Authorization Server defaults unless there is a clear application-specific need.
- Keep configuration minimal and consistent with framework defaults.

### Error Handling and i18n

- Centralized domain exception handling is currently minimal compared to the original gRPC sample.
- Keep default messages in `messages.properties`; add Turkish translations in `messages_tr.properties`.
- Avoid adding custom exception layers unless they serve application-specific behavior that Spring Authorization Server does not already provide.

### Transaction Management

- Put `@Transactional` on service methods when introducing explicit service-layer logic.
- Use `@Transactional(readOnly = true)` for read-only paths.
- Avoid transaction annotations on configuration beans.

### Security

- Keep Authorization Server protocol endpoint security in `AuthorizationServerConfig` and application/login security in `SecurityConfig`.
- Keep user loading in `DomainUserDetailsService`.
- Keep authority constants in `AuthoritiesConstants`.
- Do not store plain text passwords or client secrets in seed data.
- Keep the Authorization Server filter chain scoped to authorization endpoints; do not collapse multiple chains into `anyRequest`.
 - Preserve the current split between HTML login redirects and non-HTML localized OAuth2 error responses.

### Database and Liquibase

- Use XML-based Liquibase changelogs.
- Use lowercase database types in changelog XML (`bigint`, `varchar`, `boolean`, `timestamp`).
- Shared Liquibase properties such as `${now}` belong in `db.changelog-master.xml`.
- Registered client seed data lives in CSV and must stay aligned with `RegisteredClientEntity`, `RegisteredClientMapper`, and Spring Authorization Server's registered-client model.
- For DB changes: add a new Liquibase XML changelog and include it from `db/changelog/db.changelog-master.xml`.
- Do not modify existing changelogs that have already been applied unless this is still local sample bootstrap work and no migration history needs preservation.
- Hibernate second-level cache uses JCache backed by Caffeine. Cache regions are configured in `config/cache/CacheConfig`.

### Docker Compose (Optional)

- Spring Boot Docker Compose integration is enabled only with the Maven profile `-Pdocker-compose`.
- Compose config: `src/main/docker/services.yml`.
- Native image compose app: `src/main/docker/app.yml`.
- The application listens on port `9090`.

## Pull Request and Commit Guidelines

- Keep changes focused; avoid drive-by refactors in the same PR.
- Prefer small, logically grouped commits; avoid `WIP` or noisy fixup commits.
- Do not commit local generated output such as `target/`.
- Do not commit secrets. The sample credentials are for local development only.
- Before opening a PR: apply formatting and run tests (`./mvnw spotless:apply` and `./mvnw test`).
- Use **Conventional Commits**:
  - `feat`: new feature
  - `fix`: bug fix
  - `docs`: documentation only
  - `test`: adding or fixing tests
  - `chore`: build, CI, or tooling changes
  - `perf`: performance improvement
  - `refactor`: code changes without feature or fix
  - `build`: changes that affect the build system
  - `ci`: CI configuration
  - `style`: code style
  - `revert`: reverts a previous commit

## Review Process & What Reviewers Look For

- All automated checks pass (build, tests, Spotless, Checkstyle).
- Changes are focused and minimal; no unrelated refactors or drive-by cleanups.
- Commit history is clean, logical, and follows Conventional Commits.
- No secrets or environment-specific values are committed.
- PR description clearly explains what changed, how to verify, and any risks.
- Tests are added or updated when behavior changes.
- Cross-cutting impacts are explicitly called out when relevant:
  - Liquibase migrations
  - Security rules (`AuthorizationServerConfig`, `SecurityConfig`)
  - Native Image / AOT hints (`NativeRuntimeHints`)
  - Helm / Terraform values

## Common Mistakes to Avoid

- Reintroducing gRPC-specific assumptions into this HTTP/OAuth2 sample.
- Moving registered client seed logic back into runtime Java code instead of Liquibase.
- Editing build output under `target/`.
- Forgetting to run `./mvnw spotless:apply` before committing.
- Forgetting that port `9090` is now the sample’s default HTTP port.
- Changing `oauth2_registered_client` seed structure without checking `RegisteredClientEntity`, mapper behavior, and Spring Security's `RegisteredClient` model.
- Adding new Liquibase resources or native-sensitive framework usage without updating runtime hints where needed.

## Frontend Build

- Keep the login UI in `frontend/`; do not move authentication logic into Next.js.
- Maven uses `frontend-maven-plugin` + Corepack to install Node/pnpm, run `pnpm typecheck`, and run `pnpm build`.
- Next.js uses static export; Spring Boot serves the generated assets and Spring Security continues to process `POST /login`.
- CSRF is intentionally disabled in this sample.

## Session persistence

- Browser authentication state uses Spring Session with the custom JPA-backed `JpaIndexedSessionRepository`.
- Keep `USER_SESSION` and `USER_SESSION_ATTRIBUTES` aligned with Spring Session JDBC schema semantics.
- OAuth authorization/token state remains in `oauth2_authorization`; do not duplicate it into session attributes.
- Session attributes use Spring's Java serialization converters, so update native serialization hints when adding new custom serializable session attribute types.
