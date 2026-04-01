package io.springlens.agent.contract;

import java.util.List;
import java.util.Map;

public record PatchProposal(
        String patchId,
        AgentActionRiskLevel riskLevel,
        String reason,
        List<String> targetFiles,
        String diff,
        String rollbackDiff,
        boolean requiresApproval,
        String originSkillId,
        Map<String, String> metadata
) {

    public PatchProposal {
        patchId = requireText(patchId, "patchId");
        riskLevel = java.util.Objects.requireNonNull(riskLevel, "riskLevel");
        reason = requireText(reason, "reason");
        targetFiles = targetFiles == null ? List.of() : List.copyOf(targetFiles);
        diff = requireText(diff, "diff");
        rollbackDiff = requireText(rollbackDiff, "rollbackDiff");
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
