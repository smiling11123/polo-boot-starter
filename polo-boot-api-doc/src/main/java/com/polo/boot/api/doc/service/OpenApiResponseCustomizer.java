package com.polo.boot.api.doc.service;

import com.polo.boot.api.doc.properties.OpenApiProperties;
import com.polo.boot.core.constant.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;

public class OpenApiResponseCustomizer implements OperationCustomizer {
    private static final String JSON_MEDIA_TYPE = "application/json";

    private final OpenApiProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public OpenApiResponseCustomizer(OpenApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        if (properties.isEnableDefaultResponses()) {
            addDefaultErrorResponses(responses);
        }

        if (properties.isEnableSecurity()) {
            customizeSecurityRequirement(operation, handlerMethod);
        }
        return operation;
    }

    private void addDefaultErrorResponses(ApiResponses responses) {
        addResponseIfMissing(responses, "400", ErrorCode.PARAM_ERROR);
        addResponseIfMissing(responses, "401", ErrorCode.UNAUTHORIZED);
        addResponseIfMissing(responses, "403", ErrorCode.FORBIDDEN);
        addResponseIfMissing(responses, "409", ErrorCode.REPEAT_SUBMIT);
        addResponseIfMissing(responses, "500", ErrorCode.SYSTEM_ERROR);
    }

    private void addResponseIfMissing(ApiResponses responses, String statusCode, ErrorCode errorCode) {
        if (responses.containsKey(statusCode)) {
            return;
        }
        responses.addApiResponse(statusCode, createErrorResponse(errorCode));
    }

    private ApiResponse createErrorResponse(ErrorCode errorCode) {
        Schema<?> errorSchema = new ObjectSchema()
                .addProperty("code", new IntegerSchema().example(errorCode.getCode()))
                .addProperty("msg", new StringSchema().example(errorCode.getMessage()))
                .addProperty("data", new ObjectSchema().nullable(true));

        return new ApiResponse()
                .description(errorCode.getMessage())
                .content(new Content()
                        .addMediaType(JSON_MEDIA_TYPE, new MediaType().schema(errorSchema)));
    }

    private void customizeSecurityRequirement(Operation operation, HandlerMethod handlerMethod) {
        if (isPublicOperation(handlerMethod)) {
            operation.setSecurity(new ArrayList<>());
            return;
        }

        List<SecurityRequirement> security = operation.getSecurity();
        if (security == null) {
            security = new ArrayList<>();
            operation.setSecurity(security);
        }

        boolean exists = security.stream()
                .anyMatch(requirement -> requirement.containsKey(properties.getSecuritySchemeName()));
        if (!exists) {
            security.add(new SecurityRequirement().addList(properties.getSecuritySchemeName()));
        }
    }

    private boolean isPublicOperation(HandlerMethod handlerMethod) {
        String[] excludedPaths = properties.getSecurityExcludedPaths();
        if (excludedPaths == null || excludedPaths.length == 0) {
            return false;
        }

        for (String requestPath : resolveRequestPaths(handlerMethod)) {
            for (String excludedPath : excludedPaths) {
                if (StringUtils.hasText(excludedPath) && pathMatcher.match(excludedPath.trim(), requestPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> resolveRequestPaths(HandlerMethod handlerMethod) {
        String[] classPaths = resolveClassPaths(handlerMethod);
        String[] methodPaths = resolveMethodPaths(handlerMethod);
        List<String> resolvedPaths = new ArrayList<>();
        for (String classPath : classPaths) {
            for (String methodPath : methodPaths) {
                resolvedPaths.add(normalizePath(classPath, methodPath));
            }
        }
        return resolvedPaths;
    }

    private String[] resolveClassPaths(HandlerMethod handlerMethod) {
        RequestMapping mapping = handlerMethod.getBeanType().getAnnotation(RequestMapping.class);
        if (mapping == null || (mapping.path().length == 0 && mapping.value().length == 0)) {
            return new String[]{""};
        }
        return mapping.path().length > 0 ? mapping.path() : mapping.value();
    }

    private String[] resolveMethodPaths(HandlerMethod handlerMethod) {
        if (handlerMethod.hasMethodAnnotation(GetMapping.class)) {
            GetMapping mapping = handlerMethod.getMethodAnnotation(GetMapping.class);
            return resolvePaths(mapping != null ? mapping.path() : new String[0], mapping != null ? mapping.value() : new String[0]);
        }
        if (handlerMethod.hasMethodAnnotation(PostMapping.class)) {
            PostMapping mapping = handlerMethod.getMethodAnnotation(PostMapping.class);
            return resolvePaths(mapping != null ? mapping.path() : new String[0], mapping != null ? mapping.value() : new String[0]);
        }
        if (handlerMethod.hasMethodAnnotation(PutMapping.class)) {
            PutMapping mapping = handlerMethod.getMethodAnnotation(PutMapping.class);
            return resolvePaths(mapping != null ? mapping.path() : new String[0], mapping != null ? mapping.value() : new String[0]);
        }
        if (handlerMethod.hasMethodAnnotation(DeleteMapping.class)) {
            DeleteMapping mapping = handlerMethod.getMethodAnnotation(DeleteMapping.class);
            return resolvePaths(mapping != null ? mapping.path() : new String[0], mapping != null ? mapping.value() : new String[0]);
        }
        if (handlerMethod.hasMethodAnnotation(PatchMapping.class)) {
            PatchMapping mapping = handlerMethod.getMethodAnnotation(PatchMapping.class);
            return resolvePaths(mapping != null ? mapping.path() : new String[0], mapping != null ? mapping.value() : new String[0]);
        }

        RequestMapping mapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
        if (mapping == null) {
            return new String[]{""};
        }
        return resolvePaths(mapping.path(), mapping.value());
    }

    private String[] resolvePaths(String[] path, String[] value) {
        if (path != null && path.length > 0) {
            return path;
        }
        if (value != null && value.length > 0) {
            return value;
        }
        return new String[]{""};
    }

    private String normalizePath(String classPath, String methodPath) {
        String combined = (StringUtils.hasText(classPath) ? classPath.trim() : "")
                + "/"
                + (StringUtils.hasText(methodPath) ? methodPath.trim() : "");
        combined = combined.replaceAll("//+", "/");
        if (!combined.startsWith("/")) {
            combined = "/" + combined;
        }
        return combined.length() > 1 && combined.endsWith("/") ? combined.substring(0, combined.length() - 1) : combined;
    }
}
