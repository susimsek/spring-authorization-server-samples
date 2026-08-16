# Spring Authorization Server Samples (Spring Boot 4 + Native)

OAuth2 Authorization Server and OpenID Connect sample application built with Spring Boot 4, Spring Authorization Server, Spring Data JPA, Liquibase, H2/PostgreSQL, Spring Security, and Hibernate second-level cache backed by Caffeine/JCache.
This image runs the app as a GraalVM native executable for fast startup and low memory usage.

This image exposes an HTTP-based authorization server on port `9090`. It serves OpenID Provider metadata, JWK Set, OAuth2 token and authorization endpoints, and actuator health probes.

## Features

- OAuth2 Authorization Server with OIDC enabled
- Authorization Code, Refresh Token, and Client Credentials grants
- JDBC-backed registered client storage
- JDBC-backed authorization and consent storage
- H2 in-memory database for local use
- PostgreSQL support for production-style runs
- XML-based Liquibase schema migrations
- CSV seed data for users, authorities, and OAuth2 clients
- JPA auditing with `Instant` `created_at` and `updated_at`
- Hibernate second-level cache via JCache + Caffeine
- Actuator liveness and readiness endpoints
- GraalVM native executable

## How to use this image

### 1. Start a PostgreSQL server

```bash
docker run --name postgresql --rm -d \
  -e POSTGRES_USER=appuser \
  -e POSTGRES_PASSWORD=appuser \
  -e POSTGRES_DB=authserversamples \
  -p 127.0.0.1:5432:5432 \
  postgres:18-alpine
```

### 2. Start the application (prod mode with PostgreSQL)

```bash
docker run --rm -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/authserversamples \
  -e SPRING_DATASOURCE_USERNAME=appuser \
  -e SPRING_DATASOURCE_PASSWORD=appuser \
  -e APP_AUTHORIZATION_SERVER_ISSUER=http://127.0.0.1:9090 \
  suayb/spring-authorization-server-samples:latest-native
```

Or with H2 in-memory:

```bash
docker run --rm -p 9090:9090 \
  suayb/spring-authorization-server-samples:latest-native
```

The server is available at:

```text
localhost:9090
```

### 3. Check health

```bash
curl http://localhost:9090/actuator/health/readiness
```

Expected response contains:

```json
{"status":"UP"}
```

### 4. Fetch discovery metadata

```bash
curl http://localhost:9090/.well-known/openid-configuration
```

### 5. Request a token

Seeded OAuth2 client:

| Client ID | Client Secret |
| --- | --- |
| `demo-client` | `demo-secret` |

Request a client credentials token:

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: en' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=client_credentials \
  -d scope=openid \
  http://localhost:9090/oauth2/token
```

Fetch the JWK Set:

```bash
curl http://localhost:9090/.well-known/jwks.json
```

Capture the access token:

```bash
TOKEN=$(curl -s -u demo-client:demo-secret \
  -H 'Accept-Language: en' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=client_credentials \
  -d scope=openid \
  http://localhost:9090/oauth2/token | jq -r '.access_token')
```

Introspect the access token:

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: en' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d token="${TOKEN}" \
  http://localhost:9090/oauth2/introspect
```

Revoke the access token:

```bash
curl -u demo-client:demo-secret \
  -H 'Accept-Language: en' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d token="${TOKEN}" \
  -d token_type_hint=access_token \
  http://localhost:9090/oauth2/revoke
```

## Environment variables

| Name | Default | Description |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `default` | Active Spring profile |
| `SERVER_PORT` | `9090` | HTTP server port |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:authserversamples;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `sa` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | (empty) | Database password |
| `SPRING_LIQUIBASE_ENABLED` | `true` | Enable or disable Liquibase migrations |
| `APP_AUTHORIZATION_SERVER_ISSUER` | `https://spring-authorization-server-samples.local` | OIDC issuer |

## HTTP endpoints

Authorization Server:

- `GET /.well-known/openid-configuration`
- `GET /.well-known/jwks.json`
- `GET /oauth2/authorize`
- `POST /oauth2/token`
- `POST /oauth2/revoke`
- `POST /oauth2/introspect`

OIDC:

- `GET /connect/logout`

Health:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

## Health checks

The image exposes standard HTTP actuator probes.

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

## Notes

- This is an HTTP OAuth2/OIDC authorization server sample; it is not a gRPC service.
- The default database is in-memory H2, so data is reset when the container stops.
- Liquibase migrations and CSV seed data run on startup by default.
- Hibernate second-level cache is enabled in the application configuration.
- Registered OAuth2 clients are seeded from Liquibase CSV, not created dynamically at startup.
- OAuth2 error responses honor `Accept-Language`; use headers such as `Accept-Language: en` on token-oriented requests when you want localized error messages.
- `authorization_code` flow requires a browser login and redirect handling, so it is not shown as a curl-only example here.
