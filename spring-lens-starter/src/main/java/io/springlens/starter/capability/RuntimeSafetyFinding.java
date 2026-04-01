package io.springlens.starter.capability;

import java.util.Objects;

public record RuntimeSafetyFinding(
        String ruleId,
        String severity,
        String beanName,
        String className,
        String fieldName,
        String fieldType,
        String message,
        String recommendation
) {

    public RuntimeSafetyFinding {
        ruleId = requireText(ruleId, "ruleId");
        severity = requireText(severity, "severity");
        beanName = requireText(beanName, "beanName");
        className = requireText(className, "className");
        fieldName = requireText(fieldName, "fieldName");
        fieldType = requireText(fieldType, "fieldType");
        message = requireText(message, "message");
        recommendation = requireText(recommendation, "recommendation");
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
