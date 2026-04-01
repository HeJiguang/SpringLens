package io.springlens.agent.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PatchProposalDraft(
        String draftId,
        AgentActionRiskLevel riskLevel,
        String templateId,
        String title,
        String reason,
        List<String> targetFiles,
        boolean requiresApproval,
        String originSkillId,
        Map<String, String> metadata
) {

    public PatchProposalDraft {
        draftId = requireText(draftId, "draftId");
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        templateId = requireText(templateId, "templateId");
        title = requireText(title, "title");
        reason = requireText(reason, "reason");
        targetFiles = targetFiles == null ? List.of() : List.copyOf(targetFiles);
        originSkillId = normalizeNullable(originSkillId);
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
