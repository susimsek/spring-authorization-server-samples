# Spring Authorization Server Samples

[![Build Status](https://circleci.com/gh/susimsek/spring-authorization-server-samples/tree/main.svg?style=shield)](https://circleci.com/gh/susimsek/spring-authorization-server-samples/tree/main)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=spring-authorization-server-samples&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=spring-authorization-server-samples)
[![Vulnerabilities](https://snyk.io/test/github/susimsek/spring-authorization-server-samples/badge.svg)](https://snyk.io/test/github/susimsek/spring-authorization-server-samples)
[![Docker Image Size](https://img.shields.io/docker/image-size/suayb/spring-authorization-server-samples/latest-native?label=Image%20Size)](https://hub.docker.com/r/suayb/spring-authorization-server-samples)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot/)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?logo=springsecurity&logoColor=white)](https://docs.spring.io/spring-security/)
[![Spring Authorization Server](https://img.shields.io/badge/Spring%20Authorization%20Server-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-authorization-server/)
[![GraalVM](https://img.shields.io/badge/GraalVM-25%2B-FF6600?logo=graalvm)](https://www.graalvm.org/)
[![OpenID Connect](https://img.shields.io/badge/OIDC-Identity%20Provider-1f6feb)](https://openid.net/connect/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Docker Compose](https://img.shields.io/badge/Docker_Compose-Orchestration-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Orchestration-326CE5?logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![Helm](https://img.shields.io/badge/Helm-Charts-0F1689?logo=helm&logoColor=white)](https://helm.sh/)
[![Terraform](https://img.shields.io/badge/Terraform-Infrastructure-623CE4?logo=terraform&logoColor=white)](https://www.terraform.io/)
[![Codex](https://custom-icon-badges.demolab.com/badge/Codex-AI%20Agent-74aa9c?&logo=openai&logoColor=white)](https://openai.com/codex/)

This repository is a Spring Boot 4.1 + Java 25 sample application built around **Spring Authorization Server**. It acts as an OAuth2 Authorization Server and OpenID Connect Provider, stores users and clients in a relational database with Liquibase-managed schema, uses Spring Data JPA and Hibernate second-level cache with Caffeine/JCache, supports H2 for local development and PostgreSQL for production-style runs, and can be compiled as a GraalVM native executable.

## Table of Contents

1. [Features](#features)
2. [Requirements](#requirements)
3. [Project Layout](#project-layout)
4. [Configuration](#configuration)
5. [Configuration and Profiles](#configuration-and-profiles)
6. [Run Locally](#run-locally)
7. [API Quick Overview](#api-quick-overview)
8. [OAuth2 and OIDC Endpoints](#oauth2-and-oidc-endpoints)
9. [Authorization Server Flows](#authorization-server-flows)
10. [Try with curl](#try-with-curl)
11. [Client Registration and Seed Data](#client-registration-and-seed-data)
12. [Database](#database)
13. [Internationalization](#internationalization)
14. [Build](#build)
15. [Performance Tests](#performance-tests)
16. [Code Quality](#code-quality)
17. [GraalVM Native Image](#graalvm-native-image)
18. [Docker Image](#docker-image)
19. [Kubernetes Health Probe](#kubernetes-health-probe)
20. [Docker Compose Support](#docker-compose-support)
21. [Helm](#helm)
22. [Terraform](#terraform)
23. [Continuous Integration](#continuous-integration)

## Features

- OAuth2 Authorization Server with OpenID Connect 1.0 enabled
- Authorization Code, Refresh Token, and Client Credentials grants
- JPA-backed registered client storage
- JPA-backed authorization and consent storage
- Form login backed by Spring Security and JPA user storage
- H2 in-memory database in PostgreSQL compatibility mode for `dev`
- PostgreSQL support for `prod`
- XML-based Liquibase schema migrations
- CSV seed data for authorities, users, user-authorities, and registered OAuth2 clients
- JPA auditing with `Instant` `created_at` and `updated_at`
- Hibernate second-level cache via JCache + Caffeine
- Spring Cache for `UserRepository#findByUsername`
- OIDC discovery metadata and JWK Set endpoints
- Actuator liveness and readiness probes
- Spotless, Checkstyle, Sonar, and JaCoCo quality gates
- GraalVM native executable build

## Requirements

- Java `25`
- Maven Wrapper (`./mvnw`)
- Kubernetes `1.24+`
- Helm `3.8.0+`
- Docker or Podman *(optional, for Jib, Docker Compose, and Helm deployments)*
- GraalVM Native Image `25+` *(optional, for native builds)*
- `curl` *(optional, for OAuth2 endpoint testing)*
- `jq` *(optional, for parsing token responses)*

## Project Layout

- Application code: `src/main/java/io/github/susimsek/springauthserversamples`
  - `config`: Spring configuration
    - `aot`: GraalVM Native Image runtime hints
    - `cache`: Spring Cache and Hibernate second-level cache configuration
    - `security`: Spring Security and Authorization Server configuration
  - `domain`: JPA entities and auditing base class
  - `repository`: Spring Data JPA repositories
  - `security`: authority constants, user details service, and security utilities
  - `web`: lightweight MVC endpoints for sample landing output
- Configuration: `src/main/resources/config`
- Liquibase changelogs: `src/main/resources/db/changelog`
- Liquibase seed data: `src/main/resources/db/data`
- i18n messages: `src/main/resources/i18n`
- Native image metadata: `src/main/resources/META-INF/native-image`
- Docker compose files: `src/main/docker`
- Helm chart: `helm/spring-authorization-server-samples`
- Tests: `src/test/java`
  - Application unit/integration tests: `src/test/java/io/github/susimsek/springauthserversamples`
  - Gatling performance tests: `src/test/java/gatling/simulations`

## Configuration

Main configuration lives in `src/main/resources/config/application.yml`.

Important defaults:

- Application name: `spring-authorization-server-samples`
- HTTP port: `9090`
- Database: `jdbc:h2:mem:authserversamples`
- JPA DDL mode: `none`
- Liquibase changelog: `classpath:db/changelog/db.changelog-master.xml`
- Default issuer: `https://spring-authorization-server-samples.local`
- Hibernate second-level cache: enabled
- Cache provider: JCache backed by Caffeine

## Configuration and Profiles

Configuration lives under `src/main/resources/config`:

- `application.yml` (shared)
- `application-dev.yml` (H2, debug logs)
- `application-prod.yml` (PostgreSQL, production cache sizing)

Maven profiles:

- `dev` (default) - H2 in-memory database, devtools
- `prod` - PostgreSQL
- `native` - GraalVM native build + Jib native-image extension
- `docker-compose` - Spring Boot Docker Compose integration

`spring.profiles.active` in `application.yml` is filled via Maven resource filtering.

Cache defaults by profile:

- `dev`: `ttl=PT1H`, `initial-capacity=50`, `maximum-size=100`
- `prod`: `ttl=PT1H`, `initial-capacity=500`, `maximum-size=1000`

## Run Locally

### Dev (H2)

Start the authorization server:

```bash
./mvnw spring-boot:run
```

The application listens on:

```text
localhost:9090
```

### Prod (PostgreSQL)

Start PostgreSQL first:

```bash
docker compose -f src/main/docker/postgresql.yml up -d
```

Then run the app with the `prod` profile:

```bash
export SPRING_DATASOURCE_USERNAME=appuser
export SPRING_DATASOURCE_PASSWORD=appuser
export APP_AUTHORIZATION_SERVER_ISSUER=http://127.0.0.1:9090
./mvnw -Pprod spring-boot:run
```

The application still listens on:

```text
localhost:9090
```

## API Quick Overview

Public infrastructure:

- `/.well-known/openid-configuration`
- `/.well-known/oauth-authorization-server`
- `/oauth2/jwks`
- `/oauth2/token`
- `/oauth2/authorize`
- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

Login and consent:

- `/login`
- `/oauth2/authorize`

OIDC:

- `/connect/logout`

## OAuth2 and OIDC Endpoints

Authorization Server endpoints:

- `GET /.well-known/oauth-authorization-server`
- `GET /.well-known/openid-configuration`
- `GET /oauth2/jwks`
- `GET /oauth2/authorize`
- `POST /oauth2/token`
- `POST /oauth2/revoke`
- `POST /oauth2/introspect`

OIDC endpoints:

- `GET /connect/logout`

Health:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

Framework endpoint families supported by Spring Authorization Server:

- Authorization Server metadata: `GET /.well-known/oauth-authorization-server`
- OpenID Provider metadata: `GET /.well-known/openid-configuration`
- JWK Set: `GET /oauth2/jwks`
- Authorization: `GET /oauth2/authorize`
- Token: `POST /oauth2/token`
- Token introspection: `POST /oauth2/introspect`
- Token revocation: `POST /oauth2/revoke`
- OIDC logout: `GET /connect/logout`
- Optional when configured: `GET /userinfo`, `POST /oauth2/par`, `POST /oauth2/device_authorization`, `GET|POST /oauth2/device_verification`, `POST /connect/register`

This sample currently focuses on metadata, JWK Set, authorization code, refresh token, client credentials, introspection, revocation, and logout.

## Authorization Server Flows

This sample behaves as an OAuth2 Authorization Server and OpenID Connect Provider. The main runtime responsibilities are:

- publish OIDC discovery metadata
- publish the active JWK Set used to verify JWTs
- authenticate end users with form login
- authenticate OAuth2 clients with client credentials
- issue access tokens and refresh tokens
- persist registered clients, authorizations, and consents in the database
- support token introspection and token revocation

### Discovery and JWK Set

Use these endpoints first when integrating a client or resource server:

- `GET /.well-known/oauth-authorization-server`
- `GET /.well-known/openid-configuration`
- `GET /oauth2/jwks`

Discovery returns the issuer, endpoint URLs, supported grant types, and other provider metadata. The JWK Set endpoint exposes the public RSA key material used to validate issued JWTs.

### Client Credentials Flow

Use this flow for machine-to-machine access where no end user is involved.

Request:

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=client_credentials \
  -d scope=openid \
  http://127.0.0.1:9090/oauth2/token
```

Behavior:

- client authentication uses HTTP Basic
- the token is issued directly from `/oauth2/token`
- no browser session, login page, or consent screen is involved
- this is the simplest flow to smoke-test the server

### Authorization Code Flow

Use this flow when an end user signs in through the authorization server.

Start authorization in a browser:

```text
http://127.0.0.1:9090/oauth2/authorize?response_type=code&client_id=demo-client&scope=openid&redirect_uri=http://127.0.0.1:8081/login/oauth2/code/demo-client
```

Then:

1. Sign in with a seeded user such as `admin/admin`.
2. Approve consent if the consent page is shown.
3. Copy the `code` query parameter from the redirect target.
4. Exchange the code for tokens with curl:

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=authorization_code \
  -d code='<AUTHORIZATION_CODE>' \
  -d redirect_uri='http://127.0.0.1:8081/login/oauth2/code/demo-client' \
  http://127.0.0.1:9090/oauth2/token
```

Notes:

- `redirect_uri` must exactly match the value used in the authorize request
- this flow is not practical as a curl-only test because login and redirect handling require a browser
- this flow is the one that produces end-user authorization and consent records

### Confidential Client with PKCE

Use this flow when a confidential client still wants PKCE protection on top of client secret authentication.

What PKCE is:

- PKCE stands for Proof Key for Code Exchange
- it adds a second proof to the authorization code flow
- the client creates a random secret called a `code_verifier`
- the client sends only a derived value called a `code_challenge` in the browser redirect
- when the client later exchanges the authorization code for tokens, it must send the original `code_verifier`
- the authorization server compares the two values and only issues tokens if they match

Why this matters:

- an authorization code passes through the browser and redirect URL
- if that code is intercepted, PKCE makes the stolen code useless without the original `code_verifier`
- this is especially important for SPAs, mobile apps, and any flow that uses a browser redirect
- even confidential clients benefit from PKCE because it protects the authorization code itself, not just the client credentials

How to think about it:

- client secret proves who the client is
- PKCE proves that the same client that started the browser redirect is the one finishing the token exchange
- in this sample, `pkce-client` uses both protections together

Seeded PKCE client:

- client ID: `pkce-client`
- client secret: `demo-secret`
- client authentication method: `client_secret_basic`
- grant types: `authorization_code`, `refresh_token`
- redirect URI: `http://127.0.0.1:8082/callback`
- scopes: `openid`, `profile`
- PKCE: required

Flow summary:

1. The client generates `code_verifier` and `code_challenge`.
2. The browser is redirected to `/oauth2/authorize` with the `code_challenge`.
3. The user signs in and approves consent.
4. The authorization server redirects back with an authorization `code`.
5. The client calls `/oauth2/token` with:
   - client authentication
   - the authorization code
   - the original `code_verifier`
6. The server validates both the client credentials and the PKCE proof before issuing tokens.

Generate a PKCE verifier and challenge:

```bash
CODE_VERIFIER=$(openssl rand -base64 96 | tr -d '=+/' | cut -c1-64)
CODE_CHALLENGE=$(printf '%s' "${CODE_VERIFIER}" | openssl dgst -binary -sha256 | openssl base64 -A | tr '+/' '-_' | tr -d '=')
```

Start authorization in a browser:

```text
http://127.0.0.1:9090/oauth2/authorize?response_type=code&client_id=pkce-client&scope=openid%20profile&code_challenge=<CODE_CHALLENGE>&code_challenge_method=S256&redirect_uri=http://127.0.0.1:8082/callback
```

Then:

1. Sign in with a seeded user such as `admin/admin`.
2. Approve consent if prompted.
3. Copy the `code` query parameter from the redirect target.
4. Exchange the code with both client authentication and the PKCE verifier:

```bash
curl -u pkce-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=authorization_code \
  -d code='<AUTHORIZATION_CODE>' \
  -d code_verifier="${CODE_VERIFIER}" \
  -d redirect_uri='http://127.0.0.1:8082/callback' \
  http://127.0.0.1:9090/oauth2/token
```

Important PKCE notes:

- PKCE does not replace the authorization code flow; it strengthens it
- `code_verifier` must match the `code_challenge` used in the authorize request
- `code_challenge_method=S256` means the challenge is the SHA-256 hash of the verifier
- `require-proof-key=true` is enabled to prevent PKCE downgrade attacks
- this sample uses a confidential client with PKCE, so the token request still uses HTTP Basic client authentication
- `demo-client` is the non-PKCE confidential client in this sample

### Refresh Token Flow

Use this flow after an earlier grant returns a refresh token.

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=refresh_token \
  -d refresh_token='<REFRESH_TOKEN>' \
  http://127.0.0.1:9090/oauth2/token
```

Behavior:

- the client re-authenticates with Basic auth
- a new access token is issued
- refresh token reuse and rotation behavior depends on the stored `token_settings`

### Token Introspection

Use introspection when a caller needs token state from the authorization server.

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d token="${TOKEN}" \
  http://127.0.0.1:9090/oauth2/introspect
```

Typical uses:

- verify whether a token is active
- inspect token metadata such as scopes or expiry
- support resource servers that do not validate JWTs locally

### Token Revocation

Use revocation to invalidate a previously issued token.

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d token="${TOKEN}" \
  -d token_type_hint=access_token \
  http://127.0.0.1:9090/oauth2/revoke
```

Behavior:

- revocation targets a previously issued token
- `token_type_hint` is optional but useful
- this is especially useful for testing the server's persisted authorization records

### Logout

OIDC logout is exposed at:

- `GET /connect/logout`

This is primarily a browser-oriented endpoint and is typically exercised together with an authenticated user session.

## Try with curl

Fetch the OIDC discovery metadata:

```bash
curl http://127.0.0.1:9090/.well-known/openid-configuration
```

Fetch the Authorization Server metadata:

```bash
curl http://127.0.0.1:9090/.well-known/oauth-authorization-server
```

Fetch the JWK Set:

```bash
curl http://127.0.0.1:9090/oauth2/jwks
```

Seeded users:

| Username | Password | Authorities |
| --- | --- | --- |
| `admin` | `admin` | `ROLE_ADMIN`, `ROLE_USER` |
| `user` | `user` | `ROLE_USER` |

Seeded OAuth2 client:

| Client ID | Client Secret | Grants |
| --- | --- | --- |
| `demo-client` | `demo-secret` | `authorization_code`, `refresh_token`, `client_credentials` |
| `pkce-client` | `demo-secret` | `authorization_code`, `refresh_token` |

Seeded client scopes:

- `pkce-client`: `openid`, `profile`
- `openid`
- `profile`
- `user.read`
- `user.write`

Localized OAuth2 error and authentication responses support `Accept-Language`. Use a header such as `Accept-Language: tr` when you want Turkish error messages in token, refresh, introspection, and revocation requests.

Get a client credentials token:

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=client_credentials \
  -d scope=openid \
  http://127.0.0.1:9090/oauth2/token
```

Capture the access token:

```bash
TOKEN=$(curl -s -u demo-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=client_credentials \
  -d scope=openid \
  http://127.0.0.1:9090/oauth2/token | jq -r '.access_token')
```

Introspect the access token:

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d token="${TOKEN}" \
  http://127.0.0.1:9090/oauth2/introspect
```

Revoke the access token:

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: tr' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d token="${TOKEN}" \
  -d token_type_hint=access_token \
  http://127.0.0.1:9090/oauth2/revoke
```

Check readiness:

```bash
curl http://127.0.0.1:9090/actuator/health/readiness
```

Notes:

- `client_credentials` is suitable for token, introspection, and revocation examples above.
- `authorization_code` flow requires a browser login and redirect handling; see the flow section above for the browser-plus-curl test sequence.

## Client Registration and Seed Data

Registered clients are loaded by Liquibase from:

```text
src/main/resources/db/data/oauth2-registered-clients.csv
```

That file seeds:

- `demo-client`
- BCrypt-encoded client secret
- allowed grant types
- redirect URI: `http://127.0.0.1:8081/login/oauth2/code/demo-client`
- post-logout redirect URI: `http://127.0.0.1:8081/`
- scopes: `openid`, `profile`, `user.read`, `user.write`
- PKCE not required
- `pkce-client`
- BCrypt-encoded client secret
- redirect URI: `http://127.0.0.1:8082/callback`
- post-logout redirect URI: `http://127.0.0.1:8082/`
- scopes: `openid`, `profile`
- PKCE required
- serialized `client_settings`
- serialized `token_settings`

Unlike the original gRPC sample, there is no runtime Java seeder for the client. The database seed is the source of truth.

## Database

Liquibase resources:

- Master changelog: `src/main/resources/db/changelog/db.changelog-master.xml`
- Changes: `src/main/resources/db/changelog/changes`
- Seed data: `src/main/resources/db/data`

Main tables:

- `users`
- `authorities`
- `user_authorities`
- `oauth2_registered_client`
- `oauth2_authorization`
- `oauth2_authorization_consent`

Development uses:

```text
jdbc:h2:mem:authserversamples;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

Production uses PostgreSQL.

## Internationalization

The sample keeps i18n message bundles under:

```text
src/main/resources/i18n
```

Current use is limited compared with the original gRPC sample, but the project structure remains aligned for localized messages and future extension.

## Build

Common commands:

```bash
./mvnw test
./mvnw failsafe:integration-test failsafe:verify
./mvnw verify
./mvnw gatling:test
./mvnw -DskipTests package
```

Integration tests only:

```bash
./mvnw failsafe:integration-test failsafe:verify
```

## Performance Tests

Performance tests are done with Gatling and are located in the `src/test/java/gatling/simulations` folder.

Shared Gatling defaults live in:

```text
src/test/java/gatling/GatlingDefaults.java
```

Run all simulations:

```bash
./mvnw gatling:test
```

The checked-in `OAuth2Simulation` exercises this project's HTTP authorization server endpoints:

- `GET /.well-known/openid-configuration`
- `GET /oauth2/jwks`
- `POST /oauth2/token`
- `POST /oauth2/introspect`

You can override common runtime parameters:

```bash
./mvnw gatling:test \
  -DhttpHost=127.0.0.1 \
  -DhttpPort=9090 \
  -Dusers=5 \
  -Dramp=1 \
  -Dduration=1 \
  -DclientId=demo-client \
  -DclientSecret=demo-secret \
  -Dscope=openid \
  -Dlocale=tr
```

The sample simulation focuses on discovery, JWK lookup, client credentials token issuance, and token introspection. Add more simulations under `gatling.simulations` if you want separate authorization code, PKCE, refresh token, revocation, or mixed workload scenarios.

## Code Quality

Formatting:

```bash
./mvnw spotless:check
./mvnw spotless:apply
```

Static analysis:

```bash
./mvnw checkstyle:check
./mvnw -Psonar sonar:sonar
```

Coverage and verification:

```bash
./mvnw test
./mvnw verify
```

## GraalVM Native Image

Build the native executable:

```bash
./mvnw -Pprod,native -DskipTests native:compile
```

Build the native container image with Jib:

```bash
./mvnw -Pprod,native -DskipTests jib:dockerBuild
```

## Docker Image

Build locally:

```bash
./mvnw -Pprod,native -DskipTests jib:dockerBuild
```

Run the native image against PostgreSQL:

```bash
docker run --rm -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/authserversamples \
  -e SPRING_DATASOURCE_USERNAME=appuser \
  -e SPRING_DATASOURCE_PASSWORD=appuser \
  -e APP_AUTHORIZATION_SERVER_ISSUER=http://127.0.0.1:9090 \
  docker.io/suayb/spring-authorization-server-samples:latest-native
```

## Kubernetes Health Probe

The sample exposes standard HTTP actuator probes.

Kubernetes example:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 9090
  initialDelaySeconds: 10
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 9090
  initialDelaySeconds: 10
  periodSeconds: 10
```

## Docker Compose Support

PostgreSQL only:

```bash
docker compose -f src/main/docker/postgresql.yml up -d
```

Spring Boot Docker Compose integration:

```bash
./mvnw -Pprod,docker-compose spring-boot:run
```

Standalone compose application:

```bash
docker compose -f src/main/docker/app.yml up
```

## Helm

Chart location:

```text
helm/spring-authorization-server-samples
```

Lint:

```bash
helm lint helm/spring-authorization-server-samples
```

Template:

```bash
helm template spring-authorization-server-samples helm/spring-authorization-server-samples
```

The chart keeps the same operational structure as the original repo, but uses:

- HTTP port `9090`
- HTTP health probes
- Authorization Server issuer configuration
- Liquibase-seeded registered clients

## Terraform

Terraform for local Kind + ingress-nginx lives under:

```text
terraform
```

Initialize:

```bash
terraform -chdir=terraform init
```

Apply:

```bash
terraform -chdir=terraform apply
```

Useful output:

```bash
terraform -chdir=terraform output -raw openid_configuration_url
```

Default ingress host port is:

```text
9090
```

## Continuous Integration

CircleCI configuration lives in:

```text
.circleci/config.yml
```

The pipeline keeps the same structure as the original sample:

- build
- unit test / verify
- native build
- Docker image publish
- Docker Hub README update

The project-specific names and image repository targets are switched to `spring-authorization-server-samples`.
