package io.springlens.starter.capability;

import java.util.Map;
import java.util.Objects;

public record RuntimeSafetyOverlaySuggestion(
        String suggestionId,
        String basedOnRuleId,
        String selectorType,
        String targetClassName,
        String targetMethodName,
        String probeId,
        String title,
        String rationale,
        Map<String, String> parameters
) {

    public RuntimeSafetyOverlaySuggestion {
        suggestionId = requireText(suggestionId, "suggestionId");
        basedOnRuleId = requireText(basedOnRuleId, "basedOnRuleId");
        selectorType = requireText(selectorType, "selectorType");
        targetClassName = requireText(targetClassName, "targetClassName");
        targetMethodName = normalizeNullable(targetMethodName);
        probeId = requireText(probeId, "probeId");
        title = requireText(title, "title");
        rationale = requireText(rationale, "rationale");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalizeNullable(Objects.requireNonNull(value, fieldName));
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
