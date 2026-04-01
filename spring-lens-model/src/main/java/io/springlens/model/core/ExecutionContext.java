package io.springlens.model.core;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ExecutionContext(
        String applicationId,
        String instanceId,
        String executionId,
        String traceId,
        String spanId,
        String parentSpanId,
        ExecutionEntrypointKind entrypointKind,
        ExecutionTransportKind transportKind,
        String serviceName,
        String environment,
        Instant startedAt,
        Map<String, String> tags,
        List<String> activeOverlayIds,
        List<String> activePatchIds,
        String captureMode
) {

    public ExecutionContext {
        applicationId = requireText(applicationId, "applicationId");
        instanceId = requireText(instanceId, "instanceId");
        executionId = requireText(executionId, "executionId");
        traceId = normalizeNullable(traceId);
        spanId = normalizeNullable(spanId);
        parentSpanId = normalizeNullable(parentSpanId);
        entrypointKind = Objects.requireNonNull(entrypointKind, "entrypointKind");
        transportKind = Objects.requireNonNull(transportKind, "transportKind");
        serviceName = normalizeNullable(serviceName);
        environment = normalizeNullable(environment);
        tags = tags == null ? Map.of() : Map.copyOf(tags);
        activeOverlayIds = activeOverlayIds == null ? List.of() : List.copyOf(activeOverlayIds);
        activePatchIds = activePatchIds == null ? List.of() : List.copyOf(activePatchIds);
        captureMode = normalizeNullable(captureMode);
    }

    public ExecutionContext(
            String applicationId,
            String instanceId,
            String executionId,
            Instant startedAt,
            Map<String, String> tags
    ) {
        this(
                applicationId,
                instanceId,
                executionId,
                executionId,
                null,
                null,
                ExecutionEntrypointKind.HTTP_SERVER,
                ExecutionTransportKind.HTTP,
                applicationId,
                null,
                startedAt,
                tags,
                List.of(),
                List.of(),
                "compatibility"
        );
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
