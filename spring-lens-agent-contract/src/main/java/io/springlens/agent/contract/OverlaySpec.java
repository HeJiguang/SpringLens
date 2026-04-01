package io.springlens.agent.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record OverlaySpec(
        String overlayId,
        AgentInstrumentationMode mode,
        AgentActionRiskLevel riskLevel,
        boolean enabled,
        String ttl,
        String selectorType,
        String targetClassName,
        String targetMethodName,
        String capturePhase,
        String probeId,
        String expression,
        String description,
        List<String> tags,
        Map<String, String> metadata
) {

    public OverlaySpec {
        overlayId = requireText(overlayId, "overlayId");
        mode = Objects.requireNonNull(mode, "mode");
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        ttl = normalizeNullable(ttl);
        selectorType = requireText(selectorType, "selectorType");
        targetClassName = normalizeNullable(targetClassName);
        targetMethodName = normalizeNullable(targetMethodName);
        capturePhase = normalizeNullable(capturePhase);
        probeId = normalizeNullable(probeId);
        expression = normalizeNullable(expression);
        description = normalizeNullable(description);
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
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
