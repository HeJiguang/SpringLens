package io.springlens.starter.capability;

import java.util.Map;
import java.util.Objects;

public record RuntimeSafetyPatchSuggestion(
        String suggestionId,
        String basedOnRuleId,
        String templateId,
        String targetClassName,
        String targetFieldName,
        String title,
        String reason,
        boolean requiresApproval,
        Map<String, String> parameters
) {

    public RuntimeSafetyPatchSuggestion {
        suggestionId = requireText(suggestionId, "suggestionId");
        basedOnRuleId = requireText(basedOnRuleId, "basedOnRuleId");
        templateId = requireText(templateId, "templateId");
        targetClassName = requireText(targetClassName, "targetClassName");
        targetFieldName = requireText(targetFieldName, "targetFieldName");
        title = requireText(title, "title");
        reason = requireText(reason, "reason");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
