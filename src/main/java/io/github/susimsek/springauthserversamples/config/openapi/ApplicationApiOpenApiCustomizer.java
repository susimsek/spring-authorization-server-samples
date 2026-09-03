package io.github.susimsek.springauthserversamples.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

@Component
final class ApplicationApiOpenApiCustomizer implements OpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        documentPageableParameters(openApi);
        documentSuccessExamples(openApi);
    }

    private static void documentPageableParameters(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .filter(operation -> operation.getParameters() != null)
                .flatMap(operation -> operation.getParameters().stream())
                .filter(parameter -> "size".equals(parameter.getName()))
                .filter(parameter -> parameter.getSchema() != null)
                .forEach(
                        parameter -> {
                            parameter.getSchema().maximum(BigDecimal.valueOf(100));
                            parameter.setDescription("Page size (maximum 100).");
                        });
    }

    private static void documentSuccessExamples(OpenAPI openApi) {
        addSuccessExample(
                openApi,
                "/api/account/profile",
                Map.of(
                        "username", "user",
                        "firstName", "Sample",
                        "lastName", "User",
                        "email", "user@example.test"));
        addSuccessExample(
                openApi,
                "/api/admin/clients",
                Map.of(
                        "content",
                        List.of(
                                Map.of(
                                        "id", "b0a80123-4567-89ab-cdef-0123456789ab",
                                        "clientId", "account-console",
                                        "clientName", "Account Console")),
                        "page",
                        Map.of("size", 20, "number", 0, "totalElements", 1)));
        addSuccessExample(
                openApi,
                "/api/admin/users",
                Map.of(
                        "content",
                        List.of(
                                Map.of(
                                        "id",
                                        1,
                                        "username",
                                        "user",
                                        "enabled",
                                        true,
                                        "roles",
                                        List.of("ROLE_USER"))),
                        "page",
                        Map.of("size", 20, "number", 0, "totalElements", 1)));
    }

    private static void addSuccessExample(OpenAPI openApi, String path, Object example) {
        if (openApi.getPaths() == null) {
            return;
        }
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
}
