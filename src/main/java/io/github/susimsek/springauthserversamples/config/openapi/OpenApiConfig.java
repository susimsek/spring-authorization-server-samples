package io.github.susimsek.springauthserversamples.config.openapi;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI groups and shared authentication schemes. Endpoint contracts live on their controllers.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String ADMIN_BEARER = "adminBearer";
    public static final String ACCOUNT_BEARER = "accountBearer";

    private final ApplicationProperties applicationProperties;

    public OpenApiConfig(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Bean
    OpenAPI authorizationServerOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Spring Authorization Server Sample API")
                                .version("v1")
                                .description(
                                        "OAuth 2.1 / OpenID Connect provider with Keycloak-style"
                                                + " Administration and Account APIs.")
                                .license(new License().name("Apache-2.0")))
                .servers(
                        List.of(
                                new Server()
                                        .url(applicationProperties.authorizationServer().issuer())
                                        .description("Configured authorization server issuer")))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        ADMIN_BEARER,
                                        bearerScheme(
                                                "Access token with the `admin-api` scope and the"
                                                        + " required administrative authority."))
                                .addSecuritySchemes(
                                        ACCOUNT_BEARER,
                                        bearerScheme("Access token with the `account-api` scope."))
                                .addSchemas(
                                        "OAuth2Error",
                                        new ObjectSchema()
                                                .addProperty("error", new StringSchema())
                                                .addProperty(
                                                        "error_description", new StringSchema())
                                                .addProperty("error_uri", new StringSchema()))
                                .addSchemas(
                                        "JwkSet",
                                        new ObjectSchema().addProperty("keys", new ObjectSchema()))
                                .addSchemas(
                                        "UserInfo",
                                        new ObjectSchema()
                                                .addProperty("sub", new StringSchema())
                                                .addProperty("name", new StringSchema())
                                                .addProperty("email", new StringSchema())));
    }

    @Bean
    GroupedOpenApi adminApiOpenApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .displayName("Administration API")
                .pathsToMatch("/api/admin/**")
                .addOpenApiCustomizer(this::documentPageableParameters)
                .addOpenApiCustomizer(this::documentSuccessExamples)
                .build();
    }

    @Bean
    GroupedOpenApi accountApiOpenApi() {
        return GroupedOpenApi.builder()
                .group("account-api")
                .displayName("Account API")
                .pathsToMatch("/api/account/**")
                .addOpenApiCustomizer(this::documentPageableParameters)
                .addOpenApiCustomizer(this::documentSuccessExamples)
                .build();
    }

    @Bean
    GroupedOpenApi oauth2OidcOpenApi() {
        return GroupedOpenApi.builder()
                .group("oauth2-oidc")
                .displayName("OAuth2 and OpenID Connect")
                .pathsToMatch(
                        "/.well-known/**", "/oauth2/**", "/userinfo", "/connect/**", "/oidc/**")
                .addOpenApiCustomizer(this::documentProtocolEndpoints)
                .build();
    }

    private void documentProtocolEndpoints(OpenAPI openApi) {
        Paths paths = openApi.getPaths() == null ? new Paths() : openApi.getPaths();
        paths.addPathItem(
                "/.well-known/openid-configuration",
                jsonGet(
                        "OpenID Provider Configuration",
                        "OpenID Connect Discovery metadata.",
                        java.util.Map.of(
                                "issuer",
                                applicationProperties.authorizationServer().issuer(),
                                "token_endpoint",
                                applicationProperties.authorizationServer().issuer()
                                        + "/oauth2/token")));
        paths.addPathItem(
                "/.well-known/oauth-authorization-server",
                jsonGet(
                        "OAuth 2.0 Authorization Server Metadata",
                        "RFC 8414 server metadata.",
                        java.util.Map.of(
                                "issuer",
                                applicationProperties.authorizationServer().issuer(),
                                "grant_types_supported",
                                List.of(
                                        "authorization_code",
                                        "refresh_token",
                                        "client_credentials"))));
        paths.addPathItem(
                "/oauth2/jwks",
                jsonGet(
                        "JSON Web Key Set",
                        "Public verification keys.",
                        java.util.Map.of(
                                "keys",
                                List.of(
                                        java.util.Map.of(
                                                "kty", "RSA", "kid", "...", "use", "sig")))));
        paths.addPathItem(
                "/oauth2/authorize",
                get(
                        "Authorization Endpoint",
                        "Starts Authorization Code with PKCE; redirects to login, consent, or the"
                                + " client callback.",
                        "302"));
        paths.addPathItem(
                "/oauth2/token",
                tokenEndpoint(
                        "Token Endpoint",
                        "Exchanges authorization codes or refresh tokens. Refresh tokens are"
                                + " rotated and cannot be reused."));
        paths.addPathItem(
                "/oauth2/introspect",
                introspectionEndpoint(
                        "Token Introspection Endpoint", "Returns active-token metadata."));
        paths.addPathItem(
                "/oauth2/revoke",
                revocationEndpoint(
                        "Token Revocation Endpoint", "Revokes an access or refresh token."));
        paths.addPathItem(
                "/userinfo",
                jsonGet(
                        "UserInfo Endpoint",
                        "Returns claims for an OIDC access token.",
                        java.util.Map.of("sub", "user", "preferred_username", "user")));
        paths.addPathItem(
                "/connect/logout",
                get("RP-Initiated Logout Endpoint", "Ends the browser SSO session.", "302"));
        openApi.setPaths(paths);
    }

    private static PathItem get(String summary, String description) {
        return get(summary, description, "200");
    }

    private static PathItem get(String summary, String description, String responseCode) {
        return new PathItem().get(operation(summary, description, responseCode));
    }

    private static PathItem jsonGet(String summary, String description, Object example) {
        Operation operation = operation(summary, description, "200");
        operation
                .getResponses()
                .get("200")
                .setContent(
                        new Content()
                                .addMediaType(
                                        "application/json",
                                        new MediaType()
                                                .schema(new ObjectSchema())
                                                .example(example)));
        return new PathItem().get(operation);
    }

    private static PathItem tokenEndpoint(String summary, String description) {
        MediaType form =
                new MediaType()
                        .schema(
                                new ObjectSchema()
                                        .addProperty("grant_type", new StringSchema())
                                        .addProperty("client_id", new StringSchema())
                                        .addProperty("code", new StringSchema())
                                        .addProperty("code_verifier", new StringSchema())
                                        .addProperty("refresh_token", new StringSchema()))
                        .addExamples(
                                "authorizationCode",
                                new Example()
                                        .summary("Authorization Code + PKCE")
                                        .value(
                                                "grant_type=authorization_code&client_id=account-console&code=...&code_verifier=..."))
                        .addExamples(
                                "refreshToken",
                                new Example()
                                        .summary("Refresh token rotation")
                                        .value(
                                                "grant_type=refresh_token&client_id=account-console&refresh_token=..."));
        RequestBody requestBody =
                new RequestBody()
                        .required(true)
                        .description("OAuth 2.0 form parameters.")
                        .content(
                                new Content()
                                        .addMediaType("application/x-www-form-urlencoded", form));
        Operation operation = operation(summary, description, "200").requestBody(requestBody);
        operation
                .getResponses()
                .get("200")
                .setContent(
                        new Content()
                                .addMediaType(
                                        "application/json",
                                        new MediaType()
                                                .schema(new ObjectSchema())
                                                .example(
                                                        java.util.Map.of(
                                                                "access_token",
                                                                "eyJraWQiOi...",
                                                                "token_type",
                                                                "Bearer",
                                                                "expires_in",
                                                                300,
                                                                "refresh_token",
                                                                "..."))));
        return new PathItem().post(operation);
    }

    private static PathItem introspectionEndpoint(String summary, String description) {
        return formPost(
                summary,
                description,
                "token=eyJraWQiOi...&token_type_hint=access_token",
                java.util.Map.of("active", true, "client_id", "account-console", "sub", "user"));
    }

    private static PathItem revocationEndpoint(String summary, String description) {
        return formPost(
                summary, description, "token=eyJraWQiOi...&token_type_hint=refresh_token", null);
    }

    private static PathItem formPost(
            String summary,
            String description,
            String exampleValue,
            java.util.Map<String, Object> response) {
        MediaType form =
                new MediaType()
                        .schema(
                                new ObjectSchema()
                                        .addProperty("token", new StringSchema())
                                        .addProperty("token_type_hint", new StringSchema()))
                        .addExamples("request", new Example().value(exampleValue));
        RequestBody requestBody =
                new RequestBody()
                        .required(true)
                        .description("OAuth 2.0 form parameters.")
                        .content(
                                new Content()
                                        .addMediaType("application/x-www-form-urlencoded", form));
        Operation operation = operation(summary, description, "200").requestBody(requestBody);
        if (response != null) {
            operation
                    .getResponses()
                    .get("200")
                    .setContent(
                            new Content()
                                    .addMediaType(
                                            "application/json",
                                            new MediaType()
                                                    .schema(new ObjectSchema())
                                                    .example(response)));
        }
        return new PathItem().post(operation);
    }

    private static Operation operation(String summary, String description, String responseCode) {
        return new Operation()
                .addTagsItem("OAuth2 / OIDC")
                .summary(summary)
                .description(description)
                .responses(
                        new ApiResponses()
                                .addApiResponse(
                                        responseCode,
                                        new ApiResponse().description("Protocol response."))
                                .addApiResponse(
                                        "400",
                                        new ApiResponse()
                                                .description("OAuth 2.0 protocol error.")
                                                .content(
                                                        new Content()
                                                                .addMediaType(
                                                                        "application/json",
                                                                        new MediaType()
                                                                                .schema(
                                                                                        new ObjectSchema())
                                                                                .example(
                                                                                        java.util
                                                                                                .Map
                                                                                                .of(
                                                                                                        "error",
                                                                                                        "invalid_request",
                                                                                                        "error_description",
                                                                                                        "The request"
                                                                                                            + " is invalid."))))));
    }

    private void documentPageableParameters(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths()
                .values()
                .forEach(
                        pathItem ->
                                pathItem.readOperations()
                                        .forEach(
                                                operation -> {
                                                    if (operation.getParameters() == null) {
                                                        return;
                                                    }
                                                    operation
                                                            .getParameters()
                                                            .forEach(
                                                                    parameter -> {
                                                                        if ("size"
                                                                                        .equals(
                                                                                                parameter
                                                                                                        .getName())
                                                                                && parameter
                                                                                                .getSchema()
                                                                                        != null) {
                                                                            parameter
                                                                                    .getSchema()
                                                                                    .maximum(
                                                                                            java
                                                                                                    .math
                                                                                                    .BigDecimal
                                                                                                    .valueOf(
                                                                                                            100));
                                                                            parameter
                                                                                    .setDescription(
                                                                                            "Page size"
                                                                                                + " (maximum"
                                                                                                + " 100).");
                                                                        }
                                                                    });
                                                }));
    }

    private void documentSuccessExamples(OpenAPI openApi) {
        addSuccessExample(
                openApi,
                "/api/account/profile",
                java.util.Map.of(
                        "username", "user",
                        "firstName", "Sample",
                        "lastName", "User",
                        "email", "user@example.test"));
        addSuccessExample(
                openApi,
                "/api/admin/clients",
                java.util.Map.of(
                        "content",
                        List.of(
                                java.util.Map.of(
                                        "id", "b0a80123-4567-89ab-cdef-0123456789ab",
                                        "clientId", "account-console",
                                        "clientName", "Account Console")),
                        "page",
                        java.util.Map.of("size", 20, "number", 0, "totalElements", 1)));
        addSuccessExample(
                openApi,
                "/api/admin/users",
                java.util.Map.of(
                        "content",
                        List.of(
                                java.util.Map.of(
                                        "id",
                                        1,
                                        "username",
                                        "user",
                                        "enabled",
                                        true,
                                        "roles",
                                        List.of("ROLE_USER"))),
                        "page",
                        java.util.Map.of("size", 20, "number", 0, "totalElements", 1)));
    }

    private static void addSuccessExample(OpenAPI openApi, String path, Object example) {
        PathItem pathItem = openApi.getPaths().get(path);
        if (pathItem == null
                || pathItem.getGet() == null
                || pathItem.getGet().getResponses() == null) {
            return;
        }
        ApiResponse response = pathItem.getGet().getResponses().get("200");
        if (response == null) {
            return;
        }
        Content content = response.getContent();
        if (content == null) {
            content = new Content();
            response.setContent(content);
        }
        MediaType mediaType = content.get("application/json");
        if (mediaType == null) {
            mediaType = new MediaType();
            content.addMediaType("application/json", mediaType);
        }
        mediaType.addExamples("success", new Example().value(example));
    }

    private static SecurityScheme bearerScheme(String description) {
        return new SecurityScheme()
                .type(Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(description);
    }
}
